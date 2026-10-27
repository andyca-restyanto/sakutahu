// filepath: /backend/src/test/java/com/example/app/modules/receipt/ReceiptScanIntegrationTest.java
package com.example.app.modules.receipt;

import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.category.dto.CategoryResponse;
import com.example.app.modules.category.service.CategoryService;
import com.example.app.modules.receipt.entity.ReceiptScan;
import com.example.app.modules.receipt.entity.ReceiptScanStatus;
import com.example.app.modules.receipt.repository.ReceiptScanRepository;
import com.example.app.modules.receipt.service.ReceiptReader;
import com.example.app.modules.receipt.service.ReceiptResult;
import com.example.app.modules.telegram.entity.TelegramAccount;
import com.example.app.modules.telegram.repository.TelegramAccountRepository;
import com.example.app.modules.transaction.entity.Transaction;
import com.example.app.modules.transaction.repository.TransactionRepository;
import com.example.app.shared.security.JwtService;
import com.example.app.shared.settings.AppSettingsService;
import com.example.app.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Foto struk lewat webhook → transaksi Belanja Pribadi, dengan akses VIP/Free & kuota. ReceiptReader di-mock. */
@TestPropertySource(properties = "app.receipt.api-key=test-key")
class ReceiptScanIntegrationTest extends PostgresIntegrationTest {

    /** JPEG minimal (magic bytes FF D8 FF + padding). */
    private static final byte[] JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    @MockitoBean ReceiptReader receiptReader;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired AppSettingsService settings;
    @Autowired UserRepository userRepository;
    @Autowired TelegramAccountRepository telegramAccountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired ReceiptScanRepository scanRepository;
    @Autowired CategoryService categoryService;
    @Autowired JwtService jwtService;

    private User user;
    private long tgUserId;
    private long messageSeq = 5000;
    private Long personalShoppingId;

    @BeforeEach
    void setUp() throws Exception {
        personalShoppingId = categoryService.list(null).stream()
                .filter(c -> c.code().equals("PERSONAL_SHOPPING")).map(CategoryResponse::id).findFirst().orElseThrow();
        user = new User("rcpt-" + UUID.randomUUID() + "@mail.com", "x", "Budi", "IDR", "Asia/Jakarta");
        user.markEmailVerified(Instant.now());
        user = userRepository.save(user);
        tgUserId = ThreadLocalRandom.current().nextLong(1_000_000, 9_000_000_000L);
        TelegramAccount account = new TelegramAccount(user.getId());
        account.link(tgUserId, tgUserId, "budi", Instant.now());
        telegramAccountRepository.save(account);

        reset(telegramBotClient);
        when(telegramBotClient.sendMessage(anyLong(), anyString(), any())).thenReturn(Optional.of(777L));
        when(telegramBotClient.downloadFile(anyString(), anyLong())).thenReturn(JPEG);
        setAccess("VIP_ONLY", 3);
    }

    @AfterEach
    void restoreSettings() {
        setAccess("VIP_ONLY", 3);
    }

    @Test
    void vipReceiptIsRecordedAsPersonalShoppingToday() throws Exception {
        makeVip();
        when(receiptReader.read(any(), eq("image/jpeg"))).thenReturn(
                new ReceiptResult(true, new BigDecimal("125400"), "IDR", "Indomaret Kebayoran", LocalDate.of(2026, 10, 12), true));

        long msgId = nextId();
        sendPhoto(msgId, "file-" + msgId, null);

        Transaction tx = awaitTransaction();
        assertThat(tx.getAmount()).isEqualByComparingTo("125400");
        assertThat(tx.getCategoryId()).isEqualTo(personalShoppingId);
        assertThat(tx.getOccurredOn()).isEqualTo(LocalDate.now(ZoneId.of("Asia/Jakarta")));
        assertThat(tx.getNote()).isEqualTo("Indomaret Kebayoran");
        assertThat(tx.getReceiptScanId()).isNotNull();

        verify(telegramBotClient, timeout(5000)).editMessageText(eq(tgUserId), eq(777L),
                contains("Tercatat dari struk"), any());

        // Foto yang sama dikirim lagi → tidak tercatat dua kali
        sendPhoto(nextId(), "file-" + msgId, null);
        Thread.sleep(300);
        assertThat(receiptTxs()).hasSize(1);
    }

    @Test
    void freeUserIsRejectedWhenVipOnly() throws Exception {
        JsonNode reply = sendPhoto(nextId(), "file-free", null);
        assertThat(reply.get("text").asText()).contains("khusus VIP");
        assertThat(scansOfUser()).isEmpty();
    }

    @Test
    void freeUserGetsThreeReceiptsPerMonthWhenAccessIsAll() throws Exception {
        setAccess("ALL", 3);
        when(receiptReader.read(any(), any())).thenReturn(
                new ReceiptResult(true, new BigDecimal("10000"), "IDR", "Toko", null, true));

        for (int i = 0; i < 3; i++) {
            long id = nextId();
            sendPhoto(id, "quota-" + id, null);
        }
        awaitCondition(() -> receiptTxs().size() == 3);

        JsonNode fourth = sendPhoto(nextId(), "quota-4", null);
        assertThat(fourth.get("text").asText()).contains("Kuota baca struk bulan ini habis (3/3)");

        mvc.perform(get("/api/v1/me/features").header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.receiptScan.available").value(false))
                .andExpect(jsonPath("$.receiptScan.reason").value("QUOTA_EXCEEDED"))
                .andExpect(jsonPath("$.receiptScan.monthlyQuota").value(3))
                .andExpect(jsonPath("$.receiptScan.usedThisMonth").value(3));
    }

    @Test
    void unclearTotalAsksForAmountThenRecordsTypedAmount() throws Exception {
        makeVip();
        when(receiptReader.read(any(), any())).thenReturn(
                new ReceiptResult(true, null, "IDR", "Alfamart", null, false));

        sendPhoto(nextId(), "blur-1", "belanja bulanan");
        awaitCondition(() -> scansOfUser().stream().anyMatch(s -> s.getStatus() == ReceiptScanStatus.NEEDS_AMOUNT));
        verify(telegramBotClient, timeout(5000)).editMessageText(eq(tgUserId), eq(777L), contains("tidak terbaca"), any());

        // Pesan biasa tidak dibajak
        JsonNode normal = sendText(nextId(), "kopi 20rb");
        assertThat(normal.get("text").asText()).contains("Makan & Minum");

        // Hanya nominal → dipakai untuk struk
        JsonNode done = sendText(nextId(), "250rb");
        assertThat(done.get("text").asText()).contains("Tercatat dari struk").contains("Rp250.000").contains("Belanja Pribadi");
        Transaction tx = receiptTxs().get(0);
        assertThat(tx.getNote()).isEqualTo("belanja bulanan");
    }

    @Test
    void nonReceiptPhotoFailsWithoutUsingQuota() throws Exception {
        setAccess("ALL", 3);
        when(receiptReader.read(any(), any())).thenReturn(new ReceiptResult(false, null, null, null, null, false));

        sendPhoto(nextId(), "cat-photo", null);
        awaitCondition(() -> scansOfUser().stream().anyMatch(s -> s.getStatus() == ReceiptScanStatus.FAILED));
        verify(telegramBotClient, timeout(5000)).editMessageText(eq(tgUserId), eq(777L), contains("bukan struk"), any());
        assertThat(receiptTxs()).isEmpty();

        mvc.perform(get("/api/v1/me/features").header("Authorization", "Bearer " + token()))
                .andExpect(jsonPath("$.receiptScan.usedThisMonth").value(0));
    }

    @Test
    void pdfDocumentIsRejected() throws Exception {
        makeVip();
        long id = nextId();
        Map<String, Object> message = baseMessage(id);
        message.put("document", Map.of("file_id", "pdf-1", "file_unique_id", "pdf-u1",
                "file_name", "struk.pdf", "mime_type", "application/pdf", "file_size", 1000));
        JsonNode reply = sendUpdate(message);
        assertThat(reply.get("text").asText()).contains("PDF belum didukung");
        verify(receiptReader, never()).read(any(), any());
    }

    // ---------- helpers ----------

    private void setAccess(String mode, int quota) {
        jdbc.update("UPDATE app_settings SET value = ? WHERE key = 'receipt_scan.access'", mode);
        jdbc.update("UPDATE app_settings SET value = ? WHERE key = 'receipt_scan.free_monthly_quota'", String.valueOf(quota));
        settings.invalidate();
    }

    private void makeVip() {
        jdbc.update("UPDATE users SET plan = 'VIP' WHERE id = ?", user.getId());
    }

    private String token() {
        return jwtService.generateAccessToken(user.getId(), user.getEmail());
    }

    private long nextId() {
        return ++messageSeq;
    }

    private List<ReceiptScan> scansOfUser() {
        return scanRepository.findAll().stream().filter(s -> s.getUserId().equals(user.getId())).toList();
    }

    private List<Transaction> receiptTxs() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(user.getId()) && t.getReceiptScanId() != null)
                .toList();
    }

    private Transaction awaitTransaction() throws InterruptedException {
        awaitCondition(() -> !receiptTxs().isEmpty());
        return receiptTxs().get(0);
    }

    private static void awaitCondition(Supplier<Boolean> condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Kondisi tidak terpenuhi dalam 5 detik");
    }

    private Map<String, Object> baseMessage(long id) {
        Map<String, Object> message = new HashMap<>();
        message.put("message_id", id);
        message.put("from", Map.of("id", tgUserId, "is_bot", false, "first_name", "Budi"));
        message.put("chat", Map.of("id", tgUserId, "type", "private"));
        return message;
    }

    private JsonNode sendPhoto(long id, String uniqueId, String caption) throws Exception {
        Map<String, Object> message = baseMessage(id);
        message.put("photo", List.of(
                Map.of("file_id", "small-" + uniqueId, "file_unique_id", "s-" + uniqueId, "width", 90, "height", 160, "file_size", 2000),
                Map.of("file_id", "big-" + uniqueId, "file_unique_id", uniqueId, "width", 1080, "height", 1920, "file_size", 200000)));
        if (caption != null) {
            message.put("caption", caption);
        }
        return sendUpdate(message);
    }

    private JsonNode sendText(long id, String text) throws Exception {
        Map<String, Object> message = baseMessage(id);
        message.put("text", text);
        return sendUpdate(message);
    }

    private JsonNode sendUpdate(Map<String, Object> message) throws Exception {
        String body = mvc.perform(post("/api/v1/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("update_id", message.get("message_id"), "message", message))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.isBlank() ? json.createObjectNode() : json.readTree(body);
    }
}
