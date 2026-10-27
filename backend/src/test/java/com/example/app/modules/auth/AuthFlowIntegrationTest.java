// filepath: /backend/src/test/java/com/example/app/modules/auth/AuthFlowIntegrationTest.java
package com.example.app.modules.auth;

import com.example.app.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Alur lengkap: register → login ditolak → verifikasi → login → me → refresh (rotasi & reuse detection). */
class AuthFlowIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void fullRegisterVerifyLoginRefreshFlow() throws Exception {
        String email = "budi@mail.com";

        // 1. Register → 201, tanpa token
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("fullName", "Budi Santoso", "email", "Budi@Mail.com",
                                "password", "Rahasia123", "currency", "IDR", "timezone", "Asia/Jakarta"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.verificationRequired").value(true))
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        // 2. Login sebelum verifikasi → 403 EMAIL_NOT_VERIFIED
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", "Rahasia123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        // 3. Ambil token dari email (dikirim async setelah commit)
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(5000)).sendVerificationEmail(eq(email), eq("Budi Santoso"), link.capture(), any(Duration.class));
        String token = URI.create(link.getValue()).getQuery().replace("token=", "");

        // 4. Verifikasi → 200 + login otomatis
        MvcResult verified = mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.emailVerified").value(true))
                .andExpect(jsonPath("$.user.telegramLinked").value(false))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andReturn();

        // 5. Token yang sama tidak bisa dipakai dua kali
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("token", token))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_TOKEN_INVALID"));

        // 6. Register ulang email terverifikasi → 409
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("fullName", "Orang Lain", "email", email, "password", "Rahasia456"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));

        // 7. Password salah → 401 generik
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", "SalahBanget1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        // 8. Login berhasil, lalu /me dengan Bearer token
        MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", "Rahasia123"))))
                .andExpect(status().isOk())
                .andReturn();
        String access = read(login).get("accessToken").asText();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.currency").value("IDR"));

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer rusak"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));

        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 9. Refresh merotasi token; memakai token lama lagi mencabut semua sesi
        Cookie first = verified.getResponse().getCookie("refresh_token");
        MvcResult rotated = mvc.perform(post("/api/v1/auth/refresh").cookie(first))
                .andExpect(status().isOk())
                .andReturn();
        Cookie second = rotated.getResponse().getCookie("refresh_token");
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        mvc.perform(post("/api/v1/auth/refresh").cookie(first))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        mvc.perform(post("/api/v1/auth/refresh").cookie(second))
                .andExpect(status().isUnauthorized());

        // 10. Telegram link token butuh login, webhook butuh secret
        mvc.perform(post("/api/v1/telegram/link-token").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deepLink").value(org.hamcrest.Matchers.startsWith("https://t.me/SakuTahuBot?start=")))
                .andExpect(jsonPath("$.botUsername").value("SakuTahuBot"));
        mvc.perform(get("/api/v1/telegram/status").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linked").value(false))
                .andExpect(jsonPath("$.botUsername").value("SakuTahuBot"));
        mvc.perform(post("/api/v1/telegram/link-token"))
                .andExpect(status().isUnauthorized());

        // 11. Login via Telegram sudah dihapus: endpoint tidak lagi publik
        mvc.perform(post("/api/v1/auth/telegram").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/telegram/webhook").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationErrorsAreMappedPerField() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("fullName", "", "email", "bukan-email", "password", "pendek"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
    }

    private String body(Map<String, ?> map) throws Exception {
        return json.writeValueAsString(map);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }
}
