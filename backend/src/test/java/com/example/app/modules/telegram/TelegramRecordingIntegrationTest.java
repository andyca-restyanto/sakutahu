// filepath: /backend/src/test/java/com/example/app/modules/telegram/TelegramRecordingIntegrationTest.java
package com.example.app.modules.telegram;

import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.category.dto.CategoryResponse;
import com.example.app.modules.category.service.CategoryService;
import com.example.app.modules.telegram.entity.TelegramAccount;
import com.example.app.modules.telegram.repository.TelegramAccountRepository;
import com.example.app.modules.transaction.entity.Transaction;
import com.example.app.modules.transaction.entity.TransactionSource;
import com.example.app.modules.transaction.repository.TransactionRepository;
import com.example.app.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Alur pencatatan lewat webhook: tebak kategori, idempoten, draft + tombol, koreksi diingat, hanya hari ini. */
class TelegramRecordingIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository userRepository;
    @Autowired TelegramAccountRepository telegramAccountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired CategoryService categoryService;

    private Map<String, Long> categoryIds;
    private User user;
    private long tgUserId;
    private long messageSeq = 1000;

    @BeforeEach
    void setUp() {
        categoryIds = categoryService.list(null).stream()
                .collect(Collectors.toMap(CategoryResponse::code, CategoryResponse::id));
        user = new User("tg-" + UUID.randomUUID() + "@mail.com", "x", "Budi", "IDR", "Asia/Jakarta");
        user.markEmailVerified(Instant.now());
        user = userRepository.save(user);
        tgUserId = ThreadLocalRandom.current().nextLong(1_000_000, 9_000_000_000L);
        TelegramAccount account = new TelegramAccount(user.getId());
        account.link(tgUserId, tgUserId, "budi", Instant.now());
        telegramAccountRepository.save(account);
    }

    @Test
    void keywordMatchIsSavedImmediatelyForTodayAndIsIdempotent() throws Exception {
        long msgId = nextMessageId();
        JsonNode reply = sendText(tgUserId, msgId, "kopi susu 25rb");

        assertThat(reply.get("method").asText()).isEqualTo("sendMessage");
        assertThat(reply.get("text").asText()).contains("Tercatat").contains("Rp25.000").contains("Makan & Minum");
        assertThat(reply.at("/reply_markup/inline_keyboard/0/0/callback_data").asText()).startsWith("e:");

        List<Transaction> txs = telegramTxs();
        assertThat(txs).hasSize(1);
        Transaction tx = txs.get(0);
        assertThat(tx.getCategoryId()).isEqualTo(categoryIds.get("FOOD_DRINK"));
        assertThat(tx.getOccurredOn()).isEqualTo(LocalDate.now(ZoneId.of("Asia/Jakarta")));
        assertThat(tx.getNote()).isEqualTo("kopi susu");

        // Telegram mengirim ulang update yang sama → tidak tercatat dua kali
        sendText(tgUserId, msgId, "kopi susu 25rb");
        assertThat(telegramTxs()).hasSize(1);
    }

    @Test
    void incomeAndNewCategoriesAreRecognised() throws Exception {
        sendText(tgUserId, nextMessageId(), "+gaji oktober 5jt");
        sendText(tgUserId, nextMessageId(), "parkir 5rb");
        sendText(tgUserId, nextMessageId(), "isi pertalite 30rb");

        Map<Long, Transaction> byCategory = telegramTxs().stream()
                .collect(Collectors.toMap(Transaction::getCategoryId, t -> t));
        assertThat(byCategory).containsKeys(categoryIds.get("SALARY"), categoryIds.get("PARKING"), categoryIds.get("FUEL"));
        assertThat(byCategory.get(categoryIds.get("SALARY")).getType().name()).isEqualTo("INCOME");
    }

    @Test
    void unknownCategoryCreatesDraftAndUserChoiceIsRemembered() throws Exception {
        JsonNode ask = sendText(tgUserId, nextMessageId(), "beli barang 50rb");
        assertThat(ask.get("text").asText()).contains("Masuk kategori apa");
        assertThat(telegramTxs()).isEmpty();

        // Pilih kategori "Belanja Pribadi" dari tombol
        String data = findButton(ask, "Belanja Pribadi");
        assertThat(data).startsWith("c:");
        JsonNode done = sendCallback(tgUserId, data);
        assertThat(done.get("method").asText()).isEqualTo("editMessageText");
        assertThat(done.get("text").asText()).contains("Tercatat").contains("Belanja Pribadi");
        assertThat(telegramTxs()).hasSize(1);

        // Pesan berikutnya dengan kata yang sama langsung masuk kategori pilihan user
        JsonNode next = sendText(tgUserId, nextMessageId(), "barang 10rb");
        assertThat(next.get("text").asText()).contains("Tercatat").contains("Belanja Pribadi");
        assertThat(telegramTxs()).hasSize(2);
    }

    @Test
    void changeCategoryAndCancelFromButtons() throws Exception {
        JsonNode reply = sendText(tgUserId, nextMessageId(), "tol 20rb");
        String editData = reply.at("/reply_markup/inline_keyboard/0/0/callback_data").asText();
        String cancelData = reply.at("/reply_markup/inline_keyboard/0/1/callback_data").asText();

        JsonNode choose = sendCallback(tgUserId, editData);
        String bensin = findButton(choose, "Bensin");
        JsonNode changed = sendCallback(tgUserId, bensin);
        assertThat(changed.get("text").asText()).contains("Bensin");
        assertThat(telegramTxs().get(0).getCategoryId()).isEqualTo(categoryIds.get("FUEL"));

        JsonNode cancelled = sendCallback(tgUserId, cancelData);
        assertThat(cancelled.get("text").asText()).contains("Dibatalkan");
        assertThat(telegramTxs()).isEmpty();
    }

    @Test
    void rejectsOtherDatesAndMissingAmount() throws Exception {
        assertThat(sendText(tgUserId, nextMessageId(), "kemarin kopi 20rb").get("text").asText())
                .contains("hanya untuk hari ini");
        assertThat(sendText(tgUserId, nextMessageId(), "kopi susu").get("text").asText())
                .contains("Nominalnya belum ada");
        assertThat(telegramTxs()).isEmpty();
    }

    @Test
    void commandsWork() throws Exception {
        sendText(tgUserId, nextMessageId(), "kopi 25rb");
        assertThat(sendText(tgUserId, nextMessageId(), "/hariini").get("text").asText())
                .contains("Pengeluaran: Rp25.000");
        assertThat(sendText(tgUserId, nextMessageId(), "/kategori").get("text").asText())
                .contains("Bensin").contains("Parkir").contains("Gaji & Tunjangan");
        assertThat(sendText(tgUserId, nextMessageId(), "/batal").get("text").asText()).contains("Dibatalkan");
        assertThat(telegramTxs()).isEmpty();
    }

    @Test
    void unlinkedTelegramUserCannotRecordOrPressButtons() throws Exception {
        long stranger = tgUserId + 1;
        JsonNode reply = sendText(stranger, nextMessageId(), "kopi 25rb");
        assertThat(reply.get("text").asText()).contains("belum terhubung");

        // Menekan tombol transaksi milik orang lain → diabaikan
        JsonNode own = sendText(tgUserId, nextMessageId(), "kopi 25rb");
        String cancelData = own.at("/reply_markup/inline_keyboard/0/1/callback_data").asText();
        sendCallbackRaw(stranger, cancelData).andExpect(status().isOk());
        assertThat(telegramTxs()).hasSize(1);
    }

    // ---------- helpers ----------

    private long nextMessageId() {
        return ++messageSeq;
    }

    private List<Transaction> telegramTxs() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(user.getId()) && t.getSource() == TransactionSource.TELEGRAM)
                .toList();
    }

    private JsonNode sendText(long fromId, long messageId, String text) throws Exception {
        Map<String, Object> update = Map.of(
                "update_id", messageId,
                "message", Map.of(
                        "message_id", messageId,
                        "from", Map.of("id", fromId, "is_bot", false, "first_name", "Budi"),
                        "chat", Map.of("id", fromId, "type", "private"),
                        "text", text));
        String body = mvc.perform(post("/api/v1/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private JsonNode sendCallback(long fromId, String data) throws Exception {
        return json.readTree(sendCallbackRaw(fromId, data).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions sendCallbackRaw(long fromId, String data) throws Exception {
        Map<String, Object> update = Map.of(
                "update_id", nextMessageId(),
                "callback_query", Map.of(
                        "id", "cb-" + UUID.randomUUID(),
                        "from", Map.of("id", fromId, "is_bot", false, "first_name", "Budi"),
                        "message", Map.of(
                                "message_id", 999,
                                "from", Map.of("id", 1, "is_bot", true, "first_name", "SakuTahuBot"),
                                "chat", Map.of("id", fromId, "type", "private")),
                        "data", data));
        return mvc.perform(post("/api/v1/telegram/webhook")
                .header("X-Telegram-Bot-Api-Secret-Token", "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(update)));
    }

    /** callback_data dari tombol yang teksnya mengandung label tertentu. */
    private static String findButton(JsonNode reply, String label) {
        for (JsonNode row : reply.at("/reply_markup/inline_keyboard")) {
            for (JsonNode button : row) {
                if (button.get("text").asText().contains(label)) {
                    return button.get("callback_data").asText();
                }
            }
        }
        throw new AssertionError("Tombol '" + label + "' tidak ditemukan di " + reply);
    }
}
