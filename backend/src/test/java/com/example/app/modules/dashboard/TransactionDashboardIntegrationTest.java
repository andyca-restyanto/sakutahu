// filepath: /backend/src/test/java/com/example/app/modules/dashboard/TransactionDashboardIntegrationTest.java
package com.example.app.modules.dashboard;

import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.category.dto.CategoryResponse;
import com.example.app.modules.category.service.CategoryService;
import com.example.app.shared.security.JwtService;
import com.example.app.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Transaksi lewat API → angka dashboard: total, pemisahan tabungan, isolasi antar user, tren. */
class TransactionDashboardIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository userRepository;
    @Autowired CategoryService categoryService;
    @Autowired JwtService jwtService;

    private Map<String, Long> categoryIds;

    @BeforeEach
    void loadCategories() {
        categoryIds = categoryService.list(null).stream()
                .collect(Collectors.toMap(CategoryResponse::code, CategoryResponse::id));
    }

    @Test
    void categoriesAreSeeded() throws Exception {
        String token = tokenFor(newUser("cat"));
        mvc.perform(get("/api/v1/categories").param("type", "EXPENSE").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[0].code").value("FOOD_DRINK"))
                .andExpect(jsonPath("$[0].name").value("Makan & Minum"))
                .andExpect(jsonPath("$[2].code").value("TRANSPORT"))
                .andExpect(jsonPath("$[3].code").value("FUEL"))
                .andExpect(jsonPath("$[3].name").value("Bensin"))
                .andExpect(jsonPath("$[4].code").value("PARKING"))
                .andExpect(jsonPath("$[4].name").value("Parkir"))
                .andExpect(jsonPath("$[11].code").value("SAVING_INVESTMENT"))
                .andExpect(jsonPath("$[11].kind").value("SAVING"))
                .andExpect(jsonPath("$[12].code").value("OTHER_EXPENSE"));
        mvc.perform(get("/api/v1/categories").param("type", "INCOME").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void dashboardAggregatesOnlyOwnDataAndSeparatesSaving() throws Exception {
        String a = tokenFor(newUser("dash-a"));
        String b = tokenFor(newUser("dash-b"));

        create(a, "INCOME", "SALARY", "10000000", "2025-03-01");
        create(a, "EXPENSE", "FOOD_DRINK", "50000", "2025-03-02");
        create(a, "EXPENSE", "FOOD_DRINK", "25000", "2025-03-02");
        create(a, "EXPENSE", "TRANSPORT", "25000", "2025-03-05");
        create(a, "EXPENSE", "SAVING_INVESTMENT", "1000000", "2025-03-10");
        create(a, "EXPENSE", "FOOD_DRINK", "200000", "2025-02-15");   // periode pembanding
        create(b, "EXPENSE", "FOOD_DRINK", "999999", "2025-03-02");   // user lain, tidak boleh ikut

        JsonNode d = getJson(a, "/api/v1/dashboard?from=2025-03-01&to=2025-03-31");

        assertThat(d.at("/period/granularity").asText()).isEqualTo("DAY");
        assertThat(d.at("/period/previousFrom").asText()).isEqualTo("2025-02-01");
        assertThat(d.at("/period/previousTo").asText()).isEqualTo("2025-02-28");

        assertAmount(d.at("/summary/income"), "10000000");
        assertAmount(d.at("/summary/spending"), "100000");
        assertAmount(d.at("/summary/saving"), "1000000");
        assertAmount(d.at("/summary/balance"), "8900000");
        assertAmount(d.at("/summary/savingRate"), "10.0");

        assertAmount(d.at("/comparison/spending"), "-50.0");
        assertThat(d.at("/comparison/income").isNull()).isTrue();

        JsonNode spending = d.at("/byCategory/spending");
        assertThat(spending.size()).isEqualTo(2);
        assertThat(spending.get(0).get("code").asText()).isEqualTo("FOOD_DRINK");
        assertAmount(spending.get(0).get("amount"), "75000");
        assertAmount(spending.get(0).get("percentage"), "75.0");
        assertThat(spending.get(0).get("count").asLong()).isEqualTo(2);
        assertThat(spending.findValuesAsText("code")).doesNotContain("SAVING_INVESTMENT");

        JsonNode trend = d.get("trend");
        assertThat(trend.size()).isEqualTo(31);
        assertThat(trend.get(0).get("bucket").asText()).isEqualTo("2025-03-01");
        assertAmount(trend.get(0).get("income"), "10000000");
        assertAmount(trend.get(1).get("spending"), "75000");
        assertAmount(trend.get(2).get("spending"), "0");
        assertAmount(trend.get(9).get("saving"), "1000000");

        // Rentang > 31 hari → bulanan
        JsonNode m = getJson(a, "/api/v1/dashboard?from=2025-01-01&to=2025-03-31");
        assertThat(m.at("/period/granularity").asText()).isEqualTo("MONTH");
        assertThat(m.get("trend").findValuesAsText("bucket")).containsExactly("2025-01", "2025-02", "2025-03");
        assertAmount(m.get("trend").get(1).get("spending"), "200000");
    }

    @Test
    void transactionRulesAndOwnership() throws Exception {
        String a = tokenFor(newUser("own-a"));
        String b = tokenFor(newUser("own-b"));

        // Kategori income untuk transaksi expense → 400
        mvc.perform(post("/api/v1/transactions").header("Authorization", bearer(a))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("EXPENSE", "SALARY", "1000", "2025-03-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CATEGORY_TYPE_MISMATCH"));

        // Tanggal masa depan → 400
        mvc.perform(post("/api/v1/transactions").header("Authorization", bearer(a))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("EXPENSE", "FOOD_DRINK", "1000", LocalDate.now().plusDays(2).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // Nominal 0 → 400 per field
        mvc.perform(post("/api/v1/transactions").header("Authorization", bearer(a))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("EXPENSE", "FOOD_DRINK", "0", "2025-03-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"));

        String id = create(a, "EXPENSE", "FOOD_DRINK", "15000", "2025-03-01").get("id").asText();

        // User lain tidak bisa melihat / mengubah / menghapus → 404
        mvc.perform(get("/api/v1/transactions/" + id).header("Authorization", bearer(b)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
        mvc.perform(put("/api/v1/transactions/" + id).header("Authorization", bearer(b))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("EXPENSE", "FOOD_DRINK", "1", "2025-03-01")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/transactions/" + id).header("Authorization", bearer(b)))
                .andExpect(status().isNotFound());

        // Pemilik bisa mengubah lalu menghapus
        mvc.perform(put("/api/v1/transactions/" + id).header("Authorization", bearer(a))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("EXPENSE", "TRANSPORT", "20000", "2025-03-02")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.code").value("TRANSPORT"))
                .andExpect(jsonPath("$.occurredOn").value("2025-03-02"));

        mvc.perform(get("/api/v1/transactions").param("from", "2025-03-01").param("to", "2025-03-31")
                        .header("Authorization", bearer(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].source").value("WEB"));

        mvc.perform(delete("/api/v1/transactions/" + id).header("Authorization", bearer(a)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/transactions/bukan-uuid").header("Authorization", bearer(a)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dashboardRejectsInvalidRange() throws Exception {
        String a = tokenFor(newUser("range"));
        mvc.perform(get("/api/v1/dashboard?from=2025-03-31&to=2025-03-01").header("Authorization", bearer(a)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard?from=2023-01-01&to=2025-01-01").header("Authorization", bearer(a)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/dashboard").header("Authorization", bearer(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period.from").value(
                        LocalDate.now(ZoneId.of("Asia/Jakarta")).withDayOfMonth(1).toString()));
    }

    // ---- helpers ----

    private User newUser(String prefix) {
        User user = new User(prefix + "-" + UUID.randomUUID() + "@mail.com", "x", "Tester", "IDR", "Asia/Jakarta");
        user.markEmailVerified(Instant.now());
        return userRepository.save(user);
    }

    private String tokenFor(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String body(String type, String categoryCode, String amount, String date) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("categoryId", categoryIds.get(categoryCode));
        m.put("amount", new BigDecimal(amount));
        m.put("occurredOn", date);
        m.put("note", "test");
        return json.writeValueAsString(m);
    }

    private JsonNode create(String token, String type, String code, String amount, String date) throws Exception {
        String res = mvc.perform(post("/api/v1/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(type, code, amount, date)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    private JsonNode getJson(String token, String url) throws Exception {
        String res = mvc.perform(get(url).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    private static void assertAmount(JsonNode node, String expected) {
        assertThat(node.isNumber()).as("angka, bukan %s", node).isTrue();
        assertThat(node.decimalValue()).isEqualByComparingTo(expected);
    }
}
