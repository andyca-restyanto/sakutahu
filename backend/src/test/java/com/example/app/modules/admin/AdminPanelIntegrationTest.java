// filepath: /backend/src/test/java/com/example/app/modules/admin/AdminPanelIntegrationTest.java
package com.example.app.modules.admin;

import com.example.app.modules.admin.entity.AdminRole;
import com.example.app.modules.admin.entity.AdminUser;
import com.example.app.modules.admin.repository.AdminUserRepository;
import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.telegram.entity.TelegramAccount;
import com.example.app.modules.telegram.repository.TelegramAccountRepository;
import com.example.app.shared.security.JwtService;
import com.example.app.shared.settings.AppSettingsService;
import com.example.app.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
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
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminPanelIntegrationTest extends PostgresIntegrationTest {

    static final String OWNER_INITIAL = "PasswordAwal123";
    static final String OWNER_NEW = "PasswordBaru456";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AdminUserRepository adminRepository;
    @Autowired UserRepository userRepository;
    @Autowired TelegramAccountRepository telegramAccountRepository;
    @Autowired JwtService userJwtService;
    @Autowired AppSettingsService settings;

    /** Email & IP berbeda per test agar rate limiter (singleton) tidak saling memengaruhi antar test. */
    private String ownerEmail;
    private String clientIp;

    @BeforeEach
    void resetAdmins() {
        ownerEmail = "owner-" + UUID.randomUUID() + "@sakutahu.test";
        ThreadLocalRandom r = ThreadLocalRandom.current();
        clientIp = "10." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(1, 255);
        jdbc.update("DELETE FROM admin_audit_logs");
        jdbc.update("DELETE FROM admin_invitations");
        jdbc.update("DELETE FROM admin_refresh_tokens");
        jdbc.update("UPDATE users SET blocked_by = NULL");
        jdbc.update("DELETE FROM admin_users");
        adminRepository.save(new AdminUser(ownerEmail, "Andy Owner", passwordEncoder.encode(OWNER_INITIAL),
                AdminRole.OWNER, true, null));
    }

    @AfterEach
    void restoreSettings() {
        settings.set("receipt_scan.access", "VIP_ONLY");
        settings.set("receipt_scan.free_monthly_quota", "3");
    }

    @Test
    void ownerMustChangeInitialPasswordBeforeUsingPanel() throws Exception {
        JsonNode login = login(ownerEmail, OWNER_INITIAL);
        assertThat(login.at("/admin/role").asText()).isEqualTo("OWNER");
        assertThat(login.at("/admin/mustChangePassword").asBoolean()).isTrue();
        String token = login.get("accessToken").asText();

        mvc.perform(auth(get("/api/v1/admin/users"), token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
        mvc.perform(auth(get("/api/v1/admin/auth/me"), token)).andExpect(status().isOk());

        // Password salah / sama dengan yang lama ditolak
        mvc.perform(auth(post("/api/v1/admin/auth/change-password"), token)
                        .content(body(Map.of("currentPassword", "salah12345678", "newPassword", OWNER_NEW))))
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_INVALID"));
        mvc.perform(auth(post("/api/v1/admin/auth/change-password"), token)
                        .content(body(Map.of("currentPassword", OWNER_INITIAL, "newPassword", "pendek1"))))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        String body = mvc.perform(auth(post("/api/v1/admin/auth/change-password"), token)
                        .content(body(Map.of("currentPassword", OWNER_INITIAL, "newPassword", OWNER_NEW))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin.mustChangePassword").value(false))
                .andReturn().getResponse().getContentAsString();
        String newToken = json.readTree(body).get("accessToken").asText();

        mvc.perform(auth(get("/api/v1/admin/users"), newToken)).andExpect(status().isOk());
        assertThat(login(ownerEmail, OWNER_NEW).get("accessToken").asText()).isNotBlank();
    }

    @Test
    void userAndAdminTokensAreNotInterchangeable() throws Exception {
        String adminToken = ownerToken();
        User user = createUser("tukar");
        String userToken = userJwtService.generateAccessToken(user.getId(), user.getEmail());

        mvc.perform(auth(get("/api/v1/admin/users"), userToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/v1/auth/me"), adminToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void invitationFlowCreatesAdminWhoCannotManageAdmins() throws Exception {
        String owner = ownerToken();
        String email = "siti-" + UUID.randomUUID() + "@sakutahu.test";

        String invited = mvc.perform(auth(post("/api/v1/admin/admins/invitations"), owner)
                        .content(body(Map.of("email", email, "fullName", "Siti"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invitedByName").value("Andy Owner"))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(invited).get("expired").asBoolean()).isFalse();

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(5000)).sendAdminInvitation(eq(email), eq("Siti"), eq("Andy Owner"),
                link.capture(), any(Duration.class));
        assertThat(link.getValue()).startsWith("http://localhost:5173/admin/accept-invite?token=");
        String token = URI.create(link.getValue()).getQuery().substring("token=".length());

        mvc.perform(post("/api/v1/admin/invitations/inspect").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        String accepted = mvc.perform(post("/api/v1/admin/invitations/accept").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token, "password", "PasswordSiti789"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin.role").value("ADMIN"))
                .andExpect(jsonPath("$.admin.mustChangePassword").value(false))
                .andReturn().getResponse().getContentAsString();
        String adminToken = json.readTree(accepted).get("accessToken").asText();

        // Link hanya sekali pakai
        mvc.perform(post("/api/v1/admin/invitations/accept").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token, "password", "PasswordSiti789"))))
                .andExpect(jsonPath("$.code").value("INVITATION_INVALID"));

        // ADMIN bisa kelola user, tapi tidak bisa kelola admin
        mvc.perform(auth(get("/api/v1/admin/users"), adminToken)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/admin/admins"), adminToken))
                .andExpect(status().isForbidden());
        mvc.perform(auth(post("/api/v1/admin/admins/invitations"), adminToken)
                        .content(body(Map.of("email", "x@sakutahu.test", "fullName", "X"))))
                .andExpect(status().isForbidden());

        // Email admin yang sudah ada tidak bisa diundang lagi
        mvc.perform(auth(post("/api/v1/admin/admins/invitations"), owner)
                        .content(body(Map.of("email", email, "fullName", "Siti"))))
                .andExpect(jsonPath("$.code").value("ADMIN_EMAIL_EXISTS"));
    }

    @Test
    void ownerCanDisableAdminButNotOwner() throws Exception {
        String owner = ownerToken();
        AdminUser budi = adminRepository.save(new AdminUser("budi-" + UUID.randomUUID() + "@sakutahu.test", "Budi",
                passwordEncoder.encode("PasswordBudi123"), AdminRole.ADMIN, false, null));
        String budiToken = login(budi.getEmail(), "PasswordBudi123").get("accessToken").asText();
        UUID ownerId = adminRepository.findByEmail(ownerEmail).orElseThrow().getId();

        mvc.perform(auth(patch("/api/v1/admin/admins/" + ownerId + "/status"), owner)
                        .content(body(Map.of("status", "DISABLED"))))
                .andExpect(jsonPath("$.code").value("ADMIN_OWNER_PROTECTED"));

        mvc.perform(auth(patch("/api/v1/admin/admins/" + budi.getId() + "/status"), owner)
                        .content(body(Map.of("status", "DISABLED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        // Token lama langsung tidak berlaku; login ditolak
        mvc.perform(auth(get("/api/v1/admin/users"), budiToken)).andExpect(status().isUnauthorized());
        mvc.perform(adminLogin().contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", budi.getEmail(), "password", "PasswordBudi123"))))
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void refreshRotatesAdminSession() throws Exception {
        MvcResult result = mvc.perform(adminLogin().contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", ownerEmail, "password", OWNER_INITIAL))))
                .andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("admin_refresh_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/admin/auth");
        assertThat(cookie.isHttpOnly()).isTrue();

        mvc.perform(post("/api/v1/admin/auth/refresh").cookie(cookie)).andExpect(status().isOk());
        // Cookie lama dipakai ulang → semua sesi dicabut
        mvc.perform(post("/api/v1/admin/auth/refresh").cookie(cookie))
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        // Cookie user tidak berlaku untuk admin
        mvc.perform(post("/api/v1/admin/auth/refresh").cookie(new Cookie("refresh_token", cookie.getValue())))
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void wrongPasswordFiveTimesLocksAdmin() throws Exception {
        for (int i = 1; i <= 4; i++) {
            mvc.perform(adminLogin().contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("email", ownerEmail, "password", "salah-" + i))))
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        mvc.perform(adminLogin().contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", ownerEmail, "password", "salah-5"))))
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void changePlanIsAppliedAndAudited() throws Exception {
        String owner = ownerToken();
        User user = createUser("vip");
        String userToken = userJwtService.generateAccessToken(user.getId(), user.getEmail());
        Instant until = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        mvc.perform(auth(put("/api/v1/admin/users/" + user.getId() + "/plan"), owner)
                        .content(body(Map.of("plan", "VIP", "planExpiresAt", until.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("VIP"))
                .andExpect(jsonPath("$.history[0].action").value("USER_PLAN_CHANGED"))
                .andExpect(jsonPath("$.history[0].adminName").value("Andy Owner"))
                .andExpect(jsonPath("$.history[0].detail.toPlan").value("VIP"));

        mvc.perform(get("/api/v1/me/features").header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.plan").value("VIP"));

        // Tanggal lampau ditolak
        mvc.perform(auth(put("/api/v1/admin/users/" + user.getId() + "/plan"), owner)
                        .content(body(Map.of("plan", "VIP", "planExpiresAt", "2020-01-01T00:00:00Z"))))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(auth(get("/api/v1/admin/users?plan=VIP&q=" + user.getEmail()), owner))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].plan").value("VIP"));
    }

    @Test
    void blockedUserLosesWebAndTelegramAccessUntilUnblocked() throws Exception {
        String owner = ownerToken();
        User user = createUser("blokir");
        String userToken = userJwtService.generateAccessToken(user.getId(), user.getEmail());
        long tgId = ThreadLocalRandom.current().nextLong(1_000_000, 9_000_000_000L);
        TelegramAccount account = new TelegramAccount(user.getId());
        account.link(tgId, tgId, "blokir", Instant.now());
        telegramAccountRepository.save(account);

        mvc.perform(auth(post("/api/v1/admin/users/" + user.getId() + "/block"), owner)
                        .content(body(Map.of("reason", "Spam"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true))
                .andExpect(jsonPath("$.blockedReason").value("Spam"));

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", user.getEmail(), "password", "PasswordUser1"))))
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));

        String reply = mvc.perform(post("/api/v1/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("update_id", 1, "message", Map.of(
                                "message_id", 1,
                                "from", Map.of("id", tgId, "is_bot", false, "first_name", "B"),
                                "chat", Map.of("id", tgId, "type", "private"),
                                "text", "kopi 20rb")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(reply).get("text").asText()).contains("diblokir");

        mvc.perform(auth(get("/api/v1/admin/users?status=BLOCKED&q=" + user.getEmail()), owner))
                .andExpect(jsonPath("$.totalItems").value(1));

        mvc.perform(auth(post("/api/v1/admin/users/" + user.getId() + "/unblock"), owner))
                .andExpect(jsonPath("$.blocked").value(false));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void receiptSettingsUpdateTakesEffectImmediately() throws Exception {
        String owner = ownerToken();
        User user = createUser("kuota");
        String userToken = userJwtService.generateAccessToken(user.getId(), user.getEmail());

        mvc.perform(auth(put("/api/v1/admin/settings/receipt-scan"), owner)
                        .content(body(Map.of("access", "ALL", "freeMonthlyQuota", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").value("ALL"))
                .andExpect(jsonPath("$.freeMonthlyQuota").value(5))
                .andExpect(jsonPath("$.usage.month").isNotEmpty());

        mvc.perform(get("/api/v1/me/features").header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.receiptScan.available").value(true))
                .andExpect(jsonPath("$.receiptScan.monthlyQuota").value(5));

        mvc.perform(auth(put("/api/v1/admin/settings/receipt-scan"), owner)
                        .content(body(Map.of("access", "ALL", "freeMonthlyQuota", 101))))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        Integer audits = jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_logs WHERE action = 'RECEIPT_SETTINGS_CHANGED'", Integer.class);
        assertThat(audits).isEqualTo(1);
    }

    @Test
    void revokedInvitationCannotBeUsed() throws Exception {
        String owner = ownerToken();
        String email = "rina-" + UUID.randomUUID() + "@sakutahu.test";
        String invited = mvc.perform(auth(post("/api/v1/admin/admins/invitations"), owner)
                        .content(body(Map.of("email", email, "fullName", "Rina"))))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(invited).get("id").asText();

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(5000)).sendAdminInvitation(eq(email), any(), any(), link.capture(), any());
        String token = URI.create(link.getValue()).getQuery().substring("token=".length());

        mvc.perform(auth(delete("/api/v1/admin/admins/invitations/" + id), owner))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/admin/invitations/inspect").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token))))
                .andExpect(jsonPath("$.code").value("INVITATION_INVALID"));
        mvc.perform(auth(get("/api/v1/admin/admins/invitations"), owner))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- helpers ----------

    /** Login OWNER lalu ganti password awal, kembalikan access token yang siap dipakai. */
    private String ownerToken() throws Exception {
        String initial = login(ownerEmail, OWNER_INITIAL).get("accessToken").asText();
        String body = mvc.perform(auth(post("/api/v1/admin/auth/change-password"), initial)
                        .content(body(Map.of("currentPassword", OWNER_INITIAL, "newPassword", OWNER_NEW))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = mvc.perform(adminLogin().contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private MockHttpServletRequestBuilder adminLogin() {
        return post("/api/v1/admin/auth/login").with(request -> {
            request.setRemoteAddr(clientIp);
            return request;
        });
    }

    private User createUser(String prefix) {
        User user = new User(prefix + "-" + UUID.randomUUID() + "@mail.com", passwordEncoder.encode("PasswordUser1"),
                "User " + prefix, "IDR", "Asia/Jakarta");
        user.markEmailVerified(Instant.now());
        return userRepository.save(user);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String body(Object value) throws Exception {
        return json.writeValueAsString(value);
    }
}
