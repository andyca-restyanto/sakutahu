// filepath: /backend/src/test/java/com/example/app/modules/admin/AdminPasswordResetIntegrationTest.java
package com.example.app.modules.admin;

import com.example.app.modules.admin.entity.AdminRole;
import com.example.app.modules.admin.entity.AdminUser;
import com.example.app.modules.admin.repository.AdminUserRepository;
import com.example.app.modules.auth.entity.PasswordResetToken;
import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.PasswordResetTokenRepository;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.shared.util.TokenUtils;
import com.example.app.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminPasswordResetIntegrationTest extends PostgresIntegrationTest {

    static final String OWNER_PW = "PasswordOwner123";
    static final String ADMIN_PW = "PasswordAdmin123";
    static final String NEW_PW = "PasswordBaruSekali9";
    static final String GENERIC = "Jika email terdaftar sebagai admin, link untuk mengatur ulang password telah dikirim";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AdminUserRepository adminRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository userResetTokenRepository;

    private AdminUser owner;
    private AdminUser admin;
    private String clientIp;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM admin_audit_logs");
        jdbc.update("DELETE FROM admin_invitations");
        jdbc.update("UPDATE users SET blocked_by = NULL");
        jdbc.update("DELETE FROM admin_users");
        ThreadLocalRandom r = ThreadLocalRandom.current();
        clientIp = "10." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(1, 255);
        owner = adminRepository.save(new AdminUser("owner-" + UUID.randomUUID() + "@sakutahu.test", "Andy Owner",
                passwordEncoder.encode(OWNER_PW), AdminRole.OWNER, true, null));
        admin = adminRepository.save(new AdminUser("siti-" + UUID.randomUUID() + "@sakutahu.test", "Siti",
                passwordEncoder.encode(ADMIN_PW), AdminRole.ADMIN, false, owner.getId()));
    }

    @Test
    void unknownOrDisabledAdminGetsSameAnswerAndNoEmail() throws Exception {
        String unknown = "tidakada-" + UUID.randomUUID() + "@sakutahu.test";
        forgot(unknown).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(GENERIC));
        verify(mailService, after(300).never()).sendAdminPasswordReset(eq(unknown), any(), any(), any(), any());

        jdbc.update("UPDATE admin_users SET status = 'DISABLED' WHERE id = ?", admin.getId());
        forgot(admin.getEmail()).andExpect(jsonPath("$.message").value(GENERIC));
        verify(mailService, after(300).never()).sendAdminPasswordReset(eq(admin.getEmail()), any(), any(), any(), any());
    }

    @Test
    void ownerResetsOwnPasswordAndAllSessionsEnd() throws Exception {
        Cookie oldSession = login(owner.getEmail(), OWNER_PW).getResponse().getCookie("admin_refresh_token");

        forgot(owner.getEmail().toUpperCase()).andExpect(jsonPath("$.message").value(GENERIC));
        String token = captureToken(owner.getEmail(), null);

        mvc.perform(post("/api/v1/admin/auth/reset-password/inspect").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedEmail").value(owner.getEmail().substring(0, 2) + "***@sakutahu.test"));

        // Aturan password admin: minimal 12 karakter
        mvc.perform(post("/api/v1/admin/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token, "newPassword", "Pendek12"))))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(post("/api/v1/admin/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token, "newPassword", NEW_PW))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        // Link sekali pakai, sesi lama dicabut
        mvc.perform(post("/api/v1/admin/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token, "newPassword", "PasswordLainLagi7"))))
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
        mvc.perform(post("/api/v1/admin/auth/refresh").cookie(oldSession))
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));

        // Password baru berlaku dan tidak lagi wajib ganti password awal
        JsonNode login = json.readTree(login(owner.getEmail(), NEW_PW).getResponse().getContentAsString());
        assertThat(login.at("/admin/mustChangePassword").asBoolean()).isFalse();
        mvc.perform(adminLogin().contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", owner.getEmail(), "password", OWNER_PW))))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        verify(mailService, timeout(5000)).sendAdminPasswordChanged(eq(owner.getEmail()), eq("Andy Owner"),
                anyString(), eq("http://localhost:5173/admin/forgot-password"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_logs WHERE action = 'ADMIN_PASSWORD_RESET'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void resetUnlocksLockedAdmin() throws Exception {
        jdbc.update("UPDATE admin_users SET locked_until = now() + interval '10 minutes' WHERE id = ?", admin.getId());
        forgot(admin.getEmail());
        String token = captureToken(admin.getEmail(), null);
        mvc.perform(post("/api/v1/admin/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token, "newPassword", NEW_PW))))
                .andExpect(status().isOk());
        login(admin.getEmail(), NEW_PW);
    }

    @Test
    void userResetTokenIsNotValidForAdmin() throws Exception {
        User user = new User("user-" + UUID.randomUUID() + "@mail.com", "x", "User", "IDR", "Asia/Jakarta");
        user = userRepository.save(user);
        String raw = TokenUtils.randomToken();
        userResetTokenRepository.save(new PasswordResetToken(user.getId(), TokenUtils.sha256Hex(raw),
                Instant.now().plusSeconds(600), "127.0.0.1"));

        mvc.perform(post("/api/v1/admin/auth/reset-password/inspect").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", raw))))
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }

    @Test
    void ownerCanSendResetLinkToAdminOnly() throws Exception {
        String ownerToken = ownerToken();
        String adminToken = json.readTree(login(admin.getEmail(), ADMIN_PW).getResponse().getContentAsString())
                .get("accessToken").asText();

        mvc.perform(auth(post("/api/v1/admin/admins/" + admin.getId() + "/password-reset"), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Link reset password dikirim ke si***@sakutahu.test"));
        String token = captureToken(admin.getEmail(), "Andy Owner");
        assertThat(token).isNotBlank();

        // OWNER tidak bisa memakai jalur ini untuk dirinya sendiri; ADMIN tidak bisa memanggilnya sama sekali
        mvc.perform(auth(post("/api/v1/admin/admins/" + owner.getId() + "/password-reset"), ownerToken))
                .andExpect(jsonPath("$.code").value("ADMIN_OWNER_PROTECTED"));
        mvc.perform(auth(post("/api/v1/admin/admins/" + owner.getId() + "/password-reset"), adminToken))
                .andExpect(status().isForbidden());

        Integer audits = jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_logs "
                + "WHERE action = 'ADMIN_PASSWORD_RESET_REQUESTED' AND detail ->> 'by' = 'owner'", Integer.class);
        assertThat(audits).isEqualTo(1);
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.ResultActions forgot(String email) throws Exception {
        return mvc.perform(post("/api/v1/admin/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .content(body(Map.of("email", email))));
    }

    /** Ambil token dari link email terakhir untuk alamat ini. ownerName null = diminta sendiri. */
    private String captureToken(String email, String ownerName) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        if (ownerName == null) {
            verify(mailService, timeout(5000)).sendAdminPasswordReset(eq(email), anyString(), link.capture(),
                    eq(Duration.ofMinutes(15)), isNull());
        } else {
            verify(mailService, timeout(5000)).sendAdminPasswordReset(eq(email), anyString(), link.capture(),
                    eq(Duration.ofMinutes(15)), eq(ownerName));
        }
        assertThat(link.getValue()).startsWith("http://localhost:5173/admin/reset-password?token=");
        return URI.create(link.getValue()).getQuery().substring("token=".length());
    }

    /** OWNER dibuat dengan wajib ganti password; ganti dulu agar token bisa dipakai di menu Admin. */
    private String ownerToken() throws Exception {
        String initial = json.readTree(login(owner.getEmail(), OWNER_PW).getResponse().getContentAsString())
                .get("accessToken").asText();
        String res = mvc.perform(auth(post("/api/v1/admin/auth/change-password"), initial)
                        .content(body(Map.of("currentPassword", OWNER_PW, "newPassword", NEW_PW))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("accessToken").asText();
    }

    private MvcResult login(String email, String password) throws Exception {
        return mvc.perform(adminLogin().contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private MockHttpServletRequestBuilder adminLogin() {
        return post("/api/v1/admin/auth/login").with(request -> {
            request.setRemoteAddr(clientIp);
            return request;
        });
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String body(Object value) throws Exception {
        return json.writeValueAsString(value);
    }
}
