// filepath: /backend/src/test/java/com/example/app/modules/auth/PasswordFlowIntegrationTest.java
package com.example.app.modules.auth;

import com.example.app.modules.auth.entity.PasswordResetToken;
import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.PasswordResetTokenRepository;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.shared.util.TokenUtils;
import com.example.app.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordFlowIntegrationTest extends PostgresIntegrationTest {

    static final String OLD = "PasswordLama1";
    static final String NEW = "PasswordBaru2";
    static final String GENERIC = "Jika email terdaftar, link untuk mengatur ulang password telah dikirim";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository resetTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void unknownEmailGetsSameAnswerAndNoEmail() throws Exception {
        String email = "tidakada-" + UUID.randomUUID() + "@mail.com";
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC));
        verify(mailService, after(300).never()).sendPasswordReset(eq(email), any(), any(), any());
    }

    @Test
    void resetPasswordLogsOutEverywhereAndLinkWorksOnce() throws Exception {
        User user = createUser("reset");
        Cookie oldSession = login(user.getEmail(), OLD).getResponse().getCookie("refresh_token");

        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", user.getEmail().toUpperCase()))))
                .andExpect(jsonPath("$.message").value(GENERIC));

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(5000)).sendPasswordReset(eq(user.getEmail()), eq("User reset"), link.capture(),
                eq(Duration.ofMinutes(15)));
        assertThat(link.getValue()).startsWith("http://localhost:5173/reset-password?token=");
        String token = URI.create(link.getValue()).getQuery().substring("token=".length());

        mvc.perform(post("/api/v1/auth/reset-password/inspect").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedEmail").value(user.getEmail().substring(0, 2) + "***@mail.com"));

        // Password tidak memenuhi aturan
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token, "newPassword", "pendek"))))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token, "newPassword", NEW))))
                .andExpect(status().isOk())
                // Tidak login otomatis
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        // Link hanya sekali pakai
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token, "newPassword", "PasswordLain3"))))
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));

        // Sesi lama di semua perangkat dicabut
        mvc.perform(post("/api/v1/auth/refresh").cookie(oldSession))
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", user.getEmail(), "password", OLD))))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login(user.getEmail(), NEW);

        verify(mailService, timeout(5000)).sendPasswordChanged(eq(user.getEmail()), anyString(), anyString(),
                eq(true), eq("http://localhost:5173/forgot-password"));
    }

    @Test
    void expiredLinkIsRejected() throws Exception {
        User user = createUser("expired");
        String raw = TokenUtils.randomToken();
        resetTokenRepository.save(new PasswordResetToken(user.getId(), TokenUtils.sha256Hex(raw),
                Instant.now().minusSeconds(60), "127.0.0.1"));

        mvc.perform(post("/api/v1/auth/reset-password/inspect").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", raw))))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_EXPIRED"));
        mvc.perform(post("/api/v1/auth/reset-password/inspect").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", "token-ngawur"))))
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }

    @Test
    void blockedUserDoesNotReceiveResetEmail() throws Exception {
        User user = createUser("blokir");
        user.block("Spam", null, Instant.now());
        userRepository.save(user);

        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", user.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC));
        verify(mailService, after(300).never()).sendPasswordReset(eq(user.getEmail()), any(), any(), any());
    }

    @Test
    void changePasswordKeepsThisDeviceAndLogsOutOthers() throws Exception {
        User user = createUser("ganti");
        MvcResult deviceA = login(user.getEmail(), OLD);
        MvcResult deviceB = login(user.getEmail(), OLD);
        String tokenA = json.readTree(deviceA.getResponse().getContentAsString()).get("accessToken").asText();
        Cookie cookieB = deviceB.getResponse().getCookie("refresh_token");

        mvc.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("currentPassword", "salahsalah1", "newPassword", NEW))))
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_INVALID"));
        mvc.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("currentPassword", OLD, "newPassword", OLD))))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        MvcResult changed = mvc.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("currentPassword", OLD, "newPassword", NEW))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie newCookieA = changed.getResponse().getCookie("refresh_token");
        assertThat(newCookieA).isNotNull();

        // Perangkat lain keluar, perangkat ini tetap bisa memperpanjang sesi
        mvc.perform(post("/api/v1/auth/refresh").cookie(cookieB))
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        mvc.perform(post("/api/v1/auth/refresh").cookie(newCookieA)).andExpect(status().isOk());

        String newAccess = json.readTree(changed.getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + newAccess)).andExpect(status().isOk());
        verify(mailService, timeout(5000)).sendPasswordChanged(eq(user.getEmail()), anyString(), anyString(),
                eq(false), anyString());
    }

    @Test
    void changePasswordRequiresLogin() throws Exception {
        mvc.perform(post("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("currentPassword", OLD, "newPassword", NEW))))
                .andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private User createUser(String name) {
        User user = new User(name + "-" + UUID.randomUUID() + "@mail.com", passwordEncoder.encode(OLD),
                "User " + name, "IDR", "Asia/Jakarta");
        user.markEmailVerified(Instant.now());
        return userRepository.save(user);
    }

    private MvcResult login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String body(Object value) throws Exception {
        return json.writeValueAsString(value);
    }
}
