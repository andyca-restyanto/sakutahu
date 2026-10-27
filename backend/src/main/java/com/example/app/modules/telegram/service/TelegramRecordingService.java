// filepath: /backend/src/main/java/com/example/app/modules/telegram/service/TelegramRecordingService.java
package com.example.app.modules.telegram.service;

import com.example.app.modules.category.dto.CategoryResponse;
import com.example.app.modules.category.entity.Category;
import com.example.app.modules.category.entity.CategoryKind;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.category.service.CategoryService;
import com.example.app.modules.dashboard.repository.DashboardQueryRepository;
import com.example.app.modules.telegram.dto.TelegramUpdate;
import com.example.app.modules.telegram.dto.TelegramWebhookReply;
import com.example.app.modules.telegram.dto.TelegramWebhookReply.Button;
import com.example.app.modules.telegram.entity.TelegramDraft;
import com.example.app.modules.telegram.repository.TelegramDraftRepository;
import com.example.app.modules.telegram.service.parser.MessageParser;
import com.example.app.modules.transaction.dto.TransactionResponse;
import com.example.app.modules.transaction.entity.TransactionSource;
import com.example.app.modules.transaction.service.TransactionService;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.app.modules.telegram.service.TelegramMessageFormatter.*;

/**
 * Pencatatan transaksi dari chat Telegram (akun sudah tertaut).
 * Kategori tertebak → langsung disimpan + tombol Ubah/Batalkan (10 menit).
 * Kategori tidak tertebak → draft + tombol pilihan kategori (15 menit).
 * Semua transaksi dari Telegram tercatat untuk hari ini menurut zona waktu user.
 *
 * Format callback_data (maks 64 byte): e:&lt;txId&gt; ubah · s:&lt;txId&gt;:&lt;catId&gt; simpan kategori baru ·
 * k:&lt;txId&gt; kembali · x:&lt;txId&gt; batalkan · c:&lt;draftId&gt;:&lt;catId&gt; pilih kategori draft · d:&lt;draftId&gt; buang draft.
 */
@Service
public class TelegramRecordingService {

    private static final Logger log = LoggerFactory.getLogger(TelegramRecordingService.class);

    static final Duration EDIT_WINDOW = Duration.ofMinutes(10);
    static final Duration DRAFT_TTL = Duration.ofMinutes(15);

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final CategoryMatcher categoryMatcher;
    private final TelegramDraftRepository draftRepository;
    private final DashboardQueryRepository dashboardQueries;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public TelegramRecordingService(TransactionService transactionService,
                                    CategoryService categoryService,
                                    CategoryMatcher categoryMatcher,
                                    TelegramDraftRepository draftRepository,
                                    DashboardQueryRepository dashboardQueries,
                                    RateLimiter rateLimiter,
                                    Clock clock) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.categoryMatcher = categoryMatcher;
        this.draftRepository = draftRepository;
        this.dashboardQueries = dashboardQueries;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    // ===================== Pesan teks =====================

    public TelegramWebhookReply handleMessage(UUID userId, TelegramUpdate.Message msg) {
        long chatId = msg.chat().id();
        long messageId = msg.messageId();
        String text = msg.text();
        if (text == null || text.isBlank()) {
            return TelegramWebhookReply.sendMessage(chatId, TEXT_ONLY);
        }
        if (!rateLimiter.tryConsume("tg-msg:" + msg.from().id(), 30, Duration.ofMinutes(1))) {
            return TelegramWebhookReply.sendMessage(chatId, TOO_MANY);
        }

        text = text.trim();
        if (text.startsWith("/")) {
            String command = text.split("\\s+", 2)[0].toLowerCase(Locale.ROOT).replaceAll("@.*$", "");
            switch (command) {
                case "/bantuan", "/help", "/start" -> {
                    return TelegramWebhookReply.sendMessage(chatId, HELP);
                }
                case "/kategori" -> {
                    return TelegramWebhookReply.sendMessage(chatId,
                            categoryList(categoryService.list(TransactionType.EXPENSE), categoryService.list(TransactionType.INCOME)));
                }
                case "/hariini" -> {
                    LocalDate today = transactionService.todayFor(userId);
                    return TelegramWebhookReply.sendMessage(chatId,
                            todaySummary(today, dashboardQueries.totalsByKind(userId, today, today)));
                }
                case "/batal" -> {
                    return undoLast(userId, chatId);
                }
                case "/masuk", "/keluar" -> {
                    // dilanjutkan ke parser di bawah
                }
                default -> {
                    return TelegramWebhookReply.sendMessage(chatId, HELP);
                }
            }
        }

        // Telegram bisa mengirim ulang update yang sama: jawab dengan hasil sebelumnya, jangan catat ulang
        Optional<TransactionResponse> already = transactionService.findByTelegramMessage(chatId, messageId);
        if (already.isPresent()) {
            return confirmationMessage(userId, chatId, already.get());
        }
        Optional<TelegramDraft> pendingDraft = draftRepository.findByChatIdAndSourceMessageId(chatId, messageId);
        if (pendingDraft.isPresent()) {
            return askCategoryMessage(chatId, pendingDraft.get());
        }

        MessageParser.Result parsed = MessageParser.parse(text);
        if (parsed instanceof MessageParser.DateNotAllowed) {
            return TelegramWebhookReply.sendMessage(chatId, TODAY_ONLY);
        }
        if (parsed instanceof MessageParser.MissingAmount) {
            return TelegramWebhookReply.sendMessage(chatId, NO_AMOUNT);
        }
        MessageParser.Entry entry = (MessageParser.Entry) parsed;

        Optional<Category> category = categoryMatcher.match(userId, entry.explicitType(), entry.note());
        TransactionType type = entry.explicitType() != null ? entry.explicitType()
                : category.map(Category::getType)
                .orElse(categoryMatcher.hasIncomeHint(entry.note()) ? TransactionType.INCOME : TransactionType.EXPENSE);

        if (category.isPresent()) {
            try {
                TransactionResponse tx = saveTransaction(userId, type, category.get().getId(), entry.amount(), entry.note(), chatId, messageId);
                return confirmationMessage(userId, chatId, tx);
            } catch (ApiException e) {
                return TelegramWebhookReply.sendMessage(chatId, "Gagal mencatat: " + e.getMessage());
            }
        }

        TelegramDraft draft = createDraft(userId, chatId, messageId, type, entry);
        return askCategoryMessage(chatId, draft);
    }

    // ===================== Tombol (callback_query) =====================

    public TelegramWebhookReply handleCallback(UUID userId, TelegramUpdate.CallbackQuery cq) {
        long chatId = cq.message().chat().id();
        long messageId = cq.message().messageId();
        String data = cq.data() == null ? "" : cq.data();
        String[] parts = data.split(":");

        try {
            return switch (parts[0]) {
                case "e" -> showCategoryChoice(userId, chatId, messageId, UUID.fromString(parts[1]));
                case "s" -> changeCategory(userId, chatId, messageId, UUID.fromString(parts[1]), Long.parseLong(parts[2]));
                case "k" -> backToConfirmation(userId, chatId, messageId, UUID.fromString(parts[1]));
                case "x" -> cancel(userId, chatId, messageId, UUID.fromString(parts[1]));
                case "c" -> confirmDraft(userId, chatId, messageId, Long.parseLong(parts[1]), Long.parseLong(parts[2]));
                case "d" -> discardDraft(userId, chatId, messageId, Long.parseLong(parts[1]));
                default -> TelegramWebhookReply.editMessage(chatId, messageId, HELP, null);
            };
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            log.warn("callback_data tidak dikenali: {}", data);
            return TelegramWebhookReply.editMessage(chatId, messageId, DRAFT_EXPIRED, null);
        }
    }

    private TelegramWebhookReply showCategoryChoice(UUID userId, long chatId, long messageId, UUID txId) {
        Optional<TransactionResponse> tx = findEditable(userId, txId);
        if (tx.isEmpty()) {
            return TelegramWebhookReply.editMessage(chatId, messageId, editProblem(userId, txId), null);
        }
        var keyboard = categoryKeyboard(categoryService.list(tx.get().type()), "s:" + txId + ":",
                new Button("⬅️ Kembali", "k:" + txId));
        return TelegramWebhookReply.editMessage(chatId, messageId, chooseNewCategory(tx.get()), keyboard);
    }

    private TelegramWebhookReply changeCategory(UUID userId, long chatId, long messageId, UUID txId, long categoryId) {
        Optional<TransactionResponse> editable = findEditable(userId, txId);
        if (editable.isEmpty()) {
            return TelegramWebhookReply.editMessage(chatId, messageId, editProblem(userId, txId), null);
        }
        TransactionResponse updated = transactionService.changeCategory(userId, txId, categoryId);
        // Catatan transaksi struk berisi nama toko, bukan kata kunci barang: jangan dipelajari
        if (updated.receiptScanId() == null) {
            categoryMatcher.remember(userId, updated.note(), categoryId);
        }
        return TelegramWebhookReply.editMessage(chatId, messageId,
                confirmation(updated, todayTotal(userId, updated)), confirmationButtons(updated));
    }

    private TelegramWebhookReply backToConfirmation(UUID userId, long chatId, long messageId, UUID txId) {
        Optional<TransactionResponse> tx = findOwned(userId, txId);
        if (tx.isEmpty()) {
            return TelegramWebhookReply.editMessage(chatId, messageId, NOT_FOUND, null);
        }
        boolean editable = findEditable(userId, txId).isPresent();
        return TelegramWebhookReply.editMessage(chatId, messageId, confirmation(tx.get(), todayTotal(userId, tx.get())),
                editable ? confirmationButtons(tx.get()) : null);
    }

    private TelegramWebhookReply cancel(UUID userId, long chatId, long messageId, UUID txId) {
        Optional<TransactionResponse> tx = findEditable(userId, txId);
        if (tx.isEmpty()) {
            return TelegramWebhookReply.editMessage(chatId, messageId, editProblem(userId, txId), null);
        }
        transactionService.delete(userId, txId);
        return TelegramWebhookReply.editMessage(chatId, messageId, cancelled(tx.get()), null);
    }

    private TelegramWebhookReply confirmDraft(UUID userId, long chatId, long messageId, long draftId, long categoryId) {
        Optional<TelegramDraft> found = draftRepository.findByIdAndUserId(draftId, userId);
        LocalDate today = transactionService.todayFor(userId);
        if (found.isEmpty() || !found.get().isUsable(clock.instant(), today)) {
            found.ifPresent(draftRepository::delete);
            return TelegramWebhookReply.editMessage(chatId, messageId, DRAFT_EXPIRED, null);
        }
        TelegramDraft draft = found.get();
        try {
            TransactionResponse tx = saveTransaction(userId, draft.getType(), categoryId, draft.getAmount(), draft.getNote(),
                    draft.getChatId(), draft.getSourceMessageId());
            draftRepository.delete(draft);
            categoryMatcher.remember(userId, draft.getNote(), categoryId);
            return TelegramWebhookReply.editMessage(chatId, messageId,
                    confirmation(tx, todayTotal(userId, tx)), confirmationButtons(tx));
        } catch (ApiException e) {
            return TelegramWebhookReply.editMessage(chatId, messageId, "Gagal mencatat: " + e.getMessage(), null);
        }
    }

    private TelegramWebhookReply discardDraft(UUID userId, long chatId, long messageId, long draftId) {
        draftRepository.findByIdAndUserId(draftId, userId).ifPresent(draftRepository::delete);
        return TelegramWebhookReply.editMessage(chatId, messageId, DRAFT_CANCELLED, null);
    }

    // ===================== Perintah =====================

    private TelegramWebhookReply undoLast(UUID userId, long chatId) {
        Optional<TransactionResponse> last = transactionService.lastTelegramToday(userId);
        if (last.isEmpty()) {
            return TelegramWebhookReply.sendMessage(chatId, NOTHING_TO_UNDO);
        }
        transactionService.delete(userId, last.get().id());
        return TelegramWebhookReply.sendMessage(chatId, cancelled(last.get()));
    }

    // ===================== Helper =====================

    /** Simpan transaksi; jika Telegram mengirim update ganda bersamaan, ambil yang sudah tersimpan. */
    private TransactionResponse saveTransaction(UUID userId, TransactionType type, Long categoryId, BigDecimal amount,
                                       String note, long chatId, long messageId) {
        try {
            return transactionService.createFromTelegram(userId, type, categoryId, amount, note, chatId, messageId);
        } catch (DataIntegrityViolationException e) {
            return transactionService.findByTelegramMessage(chatId, messageId).orElseThrow(() -> e);
        }
    }

    private TelegramDraft createDraft(UUID userId, long chatId, long messageId, TransactionType type,
                                      MessageParser.Entry entry) {
        try {
            return draftRepository.save(new TelegramDraft(userId, chatId, messageId, type, entry.amount(), entry.note(),
                    transactionService.todayFor(userId), clock.instant().plus(DRAFT_TTL)));
        } catch (DataIntegrityViolationException e) {
            return draftRepository.findByChatIdAndSourceMessageId(chatId, messageId).orElseThrow(() -> e);
        }
    }

    private TelegramWebhookReply confirmationMessage(UUID userId, long chatId, TransactionResponse tx) {
        boolean editable = isWithinEditWindow(tx);
        return TelegramWebhookReply.sendMessage(chatId, confirmation(tx, todayTotal(userId, tx)),
                editable ? confirmationButtons(tx) : null);
    }

    private TelegramWebhookReply askCategoryMessage(long chatId, TelegramDraft draft) {
        var keyboard = categoryKeyboard(categoryService.list(draft.getType()), "c:" + draft.getId() + ":",
                new Button("✖️ Batal", "d:" + draft.getId()));
        return TelegramWebhookReply.sendMessage(chatId, askCategory(draft.getType(), draft.getAmount(), draft.getNote()), keyboard);
    }

    /** Total hari ini untuk jenis/kind yang sama dengan transaksi yang baru dicatat. */
    private BigDecimal todayTotal(UUID userId, TransactionResponse tx) {
        LocalDate today = transactionService.todayFor(userId);
        if (!tx.occurredOn().equals(today)) {
            return null;
        }
        Map<CategoryKind, BigDecimal> totals = dashboardQueries.totalsByKind(userId, today, today);
        return totals.get(tx.category().kind());
    }

    private Optional<TransactionResponse> findOwned(UUID userId, UUID txId) {
        try {
            return Optional.of(transactionService.get(userId, txId));
        } catch (ApiException e) {
            return Optional.empty();
        }
    }

    /** Hanya transaksi Telegram milik user, maksimal 10 menit sejak dicatat. */
    private Optional<TransactionResponse> findEditable(UUID userId, UUID txId) {
        return findOwned(userId, txId).filter(this::isWithinEditWindow);
    }

    private boolean isWithinEditWindow(TransactionResponse tx) {
        Instant now = clock.instant();
        return tx.source() == TransactionSource.TELEGRAM
                && tx.createdAt() != null
                && tx.createdAt().plus(EDIT_WINDOW).isAfter(now);
    }

    private String editProblem(UUID userId, UUID txId) {
        return findOwned(userId, txId).isPresent() ? EDIT_EXPIRED : NOT_FOUND;
    }
}
