// filepath: /backend/src/test/java/com/example/app/modules/report/JournalReportIntegrationTest.java
package com.example.app.modules.report;

import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.category.dto.CategoryResponse;
import com.example.app.modules.category.service.CategoryService;
import com.example.app.shared.security.JwtService;
import com.example.app.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Buku kas bulan lalu (selalu di masa lalu, karena transaksi web tidak boleh bertanggal masa depan):
 *   sebelum periode: +1.000.000, −200.000                     → saldo awal 800.000
 *   periode: hari-1 +5.000.000, hari-1 −25.000 ("=1+1"), hari-2 tabungan −500.000, akhir bulan −100.000
 *   → saldo berjalan 5.800.000 · 5.775.000 · 5.275.000 · 5.175.000
 */
class JournalReportIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository userRepository;
    @Autowired CategoryService categoryService;
    @Autowired JwtService jwtService;

    private Map<String, Long> categoryIds;
    private LocalDate from;
    private LocalDate to;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        categoryIds = categoryService.list(null).stream()
                .collect(Collectors.toMap(CategoryResponse::code, CategoryResponse::id));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Jakarta"));
        from = today.minusMonths(1).withDayOfMonth(1);
        to = from.with(TemporalAdjusters.lastDayOfMonth());

        token = tokenFor(newUser("jurnal"));
        create(token, "INCOME", "SALARY", "1000000", from.minusDays(1), "gaji lama");
        create(token, "EXPENSE", "FOOD_DRINK", "200000", from.minusDays(10), "makan lama");
        create(token, "INCOME", "SALARY", "5000000", from, "Gaji");
        create(token, "EXPENSE", "FOOD_DRINK", "25000", from, "=1+1");
        create(token, "EXPENSE", "SAVING_INVESTMENT", "500000", from.plusDays(1), null);
        create(token, "EXPENSE", "TRANSPORT", "100000", to, "ojek");
        // Di luar periode (bulan ini) dan milik user lain: tidak boleh ikut
        create(token, "EXPENSE", "FOOD_DRINK", "77000", today, "bulan ini");
        String other = tokenFor(newUser("lain"));
        create(other, "INCOME", "SALARY", "9999999", from, "punya orang lain");
    }

    @Test
    void journalHasOpeningBalanceAndRunningBalance() throws Exception {
        JsonNode res = getJson("/api/v1/reports/journal?from=" + from + "&to=" + to);
        assertThat(res.get("filtered").asBoolean()).isFalse();
        JsonNode s = res.get("summary");
        assertAmount(s.get("openingBalance"), "800000");
        assertAmount(s.get("totalIncome"), "5000000");
        assertAmount(s.get("totalExpense"), "125000");
        assertAmount(s.get("totalSaving"), "500000");
        assertAmount(s.get("closingBalance"), "5175000");
        assertThat(s.get("entryCount").asLong()).isEqualTo(4);

        JsonNode items = res.at("/entries/items");
        assertThat(items).hasSize(4);
        assertThat(items.get(0).get("description").asText()).isEqualTo("Gaji");
        assertAmount(items.get(0).get("income"), "5000000");
        assertAmount(items.get(0).get("expense"), "0");
        assertAmount(items.get(0).get("balance"), "5800000");
        assertAmount(items.get(1).get("balance"), "5775000");
        // Tanpa catatan → keterangan = nama kategori; tabungan masuk kolom Keluar
        assertThat(items.get(2).get("description").asText()).isEqualTo("Tabungan & Investasi");
        assertThat(items.get(2).at("/category/kind").asText()).isEqualTo("SAVING");
        assertAmount(items.get(2).get("expense"), "500000");
        assertAmount(items.get(2).get("balance"), "5275000");
        assertAmount(items.get(3).get("balance"), "5175000");
    }

    @Test
    void paginationKeepsRunningBalance() throws Exception {
        JsonNode res = getJson("/api/v1/reports/journal?from=" + from + "&to=" + to + "&page=1&size=2");
        assertThat(res.at("/entries/totalPages").asInt()).isEqualTo(2);
        assertAmount(res.at("/entries/items/0/balance"), "5275000");
        assertAmount(res.at("/entries/items/1/balance"), "5175000");
    }

    @Test
    void filterHidesBalance() throws Exception {
        JsonNode res = getJson("/api/v1/reports/journal?from=" + from + "&to=" + to + "&type=EXPENSE");
        assertThat(res.get("filtered").asBoolean()).isTrue();
        assertThat(res.at("/summary/openingBalance").isNull()).isTrue();
        assertThat(res.at("/summary/closingBalance").isNull()).isTrue();
        assertAmount(res.at("/summary/totalIncome"), "0");
        assertAmount(res.at("/summary/totalExpense"), "125000");
        assertThat(res.at("/entries/items")).hasSize(3);
        assertThat(res.at("/entries/items/0/balance").isNull()).isTrue();

        JsonNode byCategory = getJson("/api/v1/reports/journal?from=" + from + "&to=" + to
                + "&categoryId=" + categoryIds.get("TRANSPORT"));
        assertThat(byCategory.at("/summary/entryCount").asLong()).isEqualTo(1);
    }

    @Test
    void invalidRangeIsRejected() throws Exception {
        mvc.perform(get("/api/v1/reports/journal?from=" + to + "&to=" + from).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REPORT_RANGE_INVALID"));
        mvc.perform(get("/api/v1/reports/journal?from=2020-01-01&to=2022-06-30").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value("REPORT_RANGE_INVALID"));
        mvc.perform(get("/api/v1/reports/journal")).andExpect(status().isUnauthorized());
    }

    @Test
    void exportProducesValidExcelWithNumbersAndSafeText() throws Exception {
        MvcResult started = mvc.perform(get("/api/v1/reports/journal/export?from=" + from + "&to=" + to)
                        .header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult done = mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"sakutahu-jurnal-" + from + "_" + to + ".xlsx\""))
                .andReturn();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(done.getResponse().getContentAsByteArray()))) {
            assertThat(wb.getSheetName(0)).isEqualTo("Jurnal");
            assertThat(wb.getSheetName(1)).isEqualTo("Ringkasan");
            Sheet journal = wb.getSheet("Jurnal");

            int header = findRow(journal, "No");
            Row opening = journal.getRow(header + 1);
            assertThat(opening.getCell(2).getStringCellValue()).isEqualTo("Saldo awal");
            assertThat(opening.getCell(8).getNumericCellValue()).isEqualTo(800000d);

            Row first = journal.getRow(header + 2);
            assertThat(first.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC); // tanggal Excel
            assertThat(first.getCell(6).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(first.getCell(6).getNumericCellValue()).isEqualTo(5000000d);

            // Catatan "=1+1" tetap teks, bukan formula
            Cell injected = journal.getRow(header + 3).getCell(2);
            assertThat(injected.getCellType()).isEqualTo(CellType.STRING);
            assertThat(injected.getStringCellValue()).isEqualTo("=1+1");
            assertThat(injected.getCellStyle().getQuotePrefixed()).isTrue();

            Row total = journal.getRow(header + 6);
            assertThat(total.getCell(2).getStringCellValue()).startsWith("Total");
            assertThat(total.getCell(6).getNumericCellValue()).isEqualTo(5000000d);
            assertThat(total.getCell(7).getNumericCellValue()).isEqualTo(625000d);
            assertThat(total.getCell(8).getNumericCellValue()).isEqualTo(5175000d);

            Sheet summary = wb.getSheet("Ringkasan");
            int recapHeader = findRow(summary, "Kategori");
            assertThat(summary.getRow(recapHeader + 1).getCell(0).getStringCellValue()).contains("Gaji");
        }
    }

    // ---------- helpers ----------

    private static int findRow(Sheet sheet, String firstCell) {
        for (Row row : sheet) {
            Cell c = row.getCell(0);
            if (c != null && c.getCellType() == CellType.STRING && firstCell.equals(c.getStringCellValue())) {
                return row.getRowNum();
            }
        }
        throw new AssertionError("Baris '" + firstCell + "' tidak ditemukan");
    }

    private User newUser(String prefix) {
        User user = new User(prefix + "-" + UUID.randomUUID() + "@mail.com", "x", "Budi " + prefix, "IDR", "Asia/Jakarta");
        user.markEmailVerified(java.time.Instant.now());
        return userRepository.save(user);
    }

    private String tokenFor(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail());
    }

    private void create(String bearer, String type, String code, String amount, LocalDate date, String note)
            throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("categoryId", categoryIds.get(code));
        m.put("amount", new BigDecimal(amount));
        m.put("occurredOn", date.toString());
        m.put("note", note);
        mvc.perform(post("/api/v1/transactions").header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(m)))
                .andExpect(status().isCreated());
    }

    private JsonNode getJson(String url) throws Exception {
        String res = mvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    private static void assertAmount(JsonNode node, String expected) {
        assertThat(node.isNumber()).as("angka, bukan %s", node).isTrue();
        assertThat(node.decimalValue()).isEqualByComparingTo(expected);
    }
}
