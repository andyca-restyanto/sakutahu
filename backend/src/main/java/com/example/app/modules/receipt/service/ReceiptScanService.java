// filepath: /backend/src/main/java/com/example/app/modules/receipt/service/ReceiptScanService.java
package com.example.app.modules.receipt.service;

import com.example.app.modules.category.entity.CategoryKind;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.dashboard.repository.DashboardQueryRepository;
import com.example.app.modules.receipt.entity.ReceiptScan;
import com.example.app.modules.receipt.entity.ReceiptScanStatus;
import com.example.app.modules.receipt.repository.ReceiptScanRepository;
import com.example.app.modules.subscription.dto.ReceiptScanAccess;
import com.example.app.modules.subscription.entity.FeatureDenyReason;
import com.example.app.modules.subscription.service.FeatureAccessService;
import com.example.app.modules.telegram.dto.TelegramUpdate;
import com.example.app.modules.telegram.dto.TelegramWebhookReply;
import com.example.app.modules.telegram.dto.TelegramWebhookReply.Button;
import com.example.app.modules.telegram.dto.TelegramWebhookReply.InlineKeyboard;
import com.example.app.modules.telegram.service.TelegramBotClient;
import com.example.app.modules.telegram.service.TelegramMessageFormatter;
import com.example.app.modules.telegram.service.parser.MessageParser;
import com.example.app.modules.transaction.dto.TransactionResponse;
import com.example.app.modules.transaction.service.TransactionService;
import com.example.app.shared.config.AsyncConfig;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Pencatatan pengeluaran dari foto struk di Telegram.
 * Alur: terima foto → cek akses/kuota → balas "Sedang membaca…" → (async) unduh + baca dengan AI vision →
 * simpan sebagai pengeluaran "Belanja Pribadi" hari ini → edit pesan menjadi konfirmasi + tombol Ubah/Batalkan.
 * Total tidak yakin → minta user mengetik nominal (NEEDS_AMOUNT, 15 menit).
 */
@Service
public class ReceiptScanService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptScanService.class);

    static final String CATEGORY_CODE = "PERSONAL_SHOPPING";
    static final Duration NEEDS_AMOUNT_TTL = Duration.ofMinutes(15);
    static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(5);
    static final Set<String> SUPPORTED_MIME = Set.of("image/jpeg", "image/png", "image/webp");

    private final ReceiptScanRepository repository;
    private final ReceiptReader reader;
    private final ReceiptProperties props;
    private final FeatureAccessService featureAccess;
    private final TransactionService transactionService;
    private final DashboardQueryRepository dashboardQueries;
    private final TelegramBotClient botClient;
    private final RateLimiter rateLimiter;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ReceiptScanService(ReceiptScanRepository repository,
                              ReceiptReader reader,
                              ReceiptProperties props,
                              FeatureAccessService featureAccess,
                              TransactionService transactionService,
                              DashboardQueryRepository dashboardQueries,
                              TelegramBotClient botClient,
                              RateLimiter rateLimiter,
                              ApplicationEventPublisher events,
                              TransactionTemplate tx,
                              Clock clock) {
        this.repository = repository;
        this.reader = reader;
        this.props = props;
        this.featureAccess = featureAccess;
        this.transactionService = transactionService;
        this.dashboardQueries = dashboardQueries;
        this.botClient = botClient;
        this.rateLimiter = rateLimiter;
        this.events = events;
        this.tx = tx;
        this.clock = clock;
    }

    // ===================== 1. Foto diterima (webhook, harus cepat) =====================

    /**
     * @return balasan langsung untuk webhook, atau kosong jika balasan dikirim lewat Bot API
     *         (pesan "Sedang membaca…" yang nanti diedit).
     */
    @Transactional
    public Optional<TelegramWebhookReply> handleImage(UUID userId, TelegramUpdate.Message msg) {
        long chatId = msg.chat().id();
        Optional<ImageRef> image = imageOf(msg);
        if (image.isEmpty()) {
            return reply(chatId, ReceiptMessages.UNSUPPORTED_FILE);
        }
        ImageRef ref = image.get();
        if (ref.size() != null && ref.size() > props.maxFileBytes()) {
            return reply(chatId, ReceiptMessages.tooLarge(props.maxFileBytes() / (1024 * 1024)));
        }

        // Foto yang sama (Telegram mengirim ulang, atau user mengirim foto yang sama lagi)
        Optional<ReceiptScan> existing = repository.findByUserIdAndFileUniqueId(userId, ref.fileUniqueId());
        if (existing.isPresent() && existing.get().getStatus() != ReceiptScanStatus.FAILED) {
            return duplicateReply(userId, chatId, existing.get());
        }

        ReceiptScanAccess access = featureAccess.receiptScan(userId);
        if (!access.available()) {
            return reply(chatId, access.reason() == FeatureDenyReason.QUOTA_EXCEEDED
                    ? ReceiptMessages.quotaExceeded(access.usedThisMonth(), access.monthlyQuota())
                    : ReceiptMessages.VIP_ONLY);
        }
        if (!props.configured()) {
            log.warn("Struk diterima tapi ANTHROPIC_API_KEY belum diisi");
            return reply(chatId, ReceiptMessages.NOT_CONFIGURED);
        }
        if (!rateLimiter.tryConsume("receipt:" + userId, 10, Duration.ofMinutes(10))) {
            return reply(chatId, ReceiptMessages.TOO_MANY);
        }

        Instant now = clock.instant();
        String caption = trimCaption(msg.caption());
        ReceiptScan scan = existing
                .map(s -> {
                    s.retry(msg.messageId(), ref.fileId(), caption, now);
                    return s;
                })
                .orElseGet(() -> new ReceiptScan(userId, chatId, msg.messageId(), ref.fileId(), ref.fileUniqueId(),
                        caption, now));
        try {
            scan = repository.saveAndFlush(scan);
        } catch (DataIntegrityViolationException e) {
            // Update yang sama datang bersamaan: biarkan yang pertama memproses
            return reply(chatId, ReceiptMessages.ALREADY_PROCESSING);
        }

        botClient.sendMessage(chatId, ReceiptMessages.READING, null).ifPresent(scan::attachBotMessage);
        events.publishEvent(new ReceiptScanQueuedEvent(scan.getId()));
        return Optional.empty();
    }

    // ===================== 2. Proses asinkron =====================

    @Async(AsyncConfig.RECEIPT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onQueued(ReceiptScanQueuedEvent event) {
        process(event.scanId());
    }

    void process(UUID scanId) {
        ReceiptScan scan = tx.execute(status -> repository.findById(scanId)
                .filter(s -> s.getStatus() == ReceiptScanStatus.PENDING)
                .map(s -> {
                    s.markProcessing();
                    return repository.save(s);
                })
                .orElse(null));
        if (scan == null) {
            return;
        }

        ReceiptResult result;
        try {
            byte[] bytes = botClient.downloadFile(scan.getFileId(), props.maxFileBytes());
            String mediaType = detectMediaType(bytes);
            if (mediaType == null) {
                fail(scan, "UNSUPPORTED_FILE", ReceiptMessages.UNSUPPORTED_FILE);
                return;
            }
            result = reader.read(bytes, mediaType);
            // bytes tidak disimpan di mana pun dan dibuang setelah method ini selesai
        } catch (TelegramBotClient.FileTooLargeException e) {
            fail(scan, "TOO_LARGE", ReceiptMessages.tooLarge(props.maxFileBytes() / (1024 * 1024)));
            return;
        } catch (ReceiptReader.ReceiptReadException e) {
            log.warn("Struk {} gagal dibaca [{}]: {}", scanId, e.kind(), e.getMessage());
            switch (e.kind()) {
                case SERVICE_CONFIG -> fail(scan, "AI_SERVICE_CONFIG", ReceiptMessages.SERVICE_UNAVAILABLE);
                case SERVICE_BUSY -> fail(scan, "AI_SERVICE_BUSY", ReceiptMessages.SERVICE_BUSY);
                case BAD_IMAGE -> fail(scan, "AI_BAD_IMAGE", ReceiptMessages.BAD_IMAGE);
                case BAD_RESPONSE -> fail(scan, "READ_FAILED", ReceiptMessages.READ_FAILED);
            }
            return;
        } catch (IOException e) {
            log.warn("Struk {} gagal diunduh dari Telegram: {}", scanId, e.getMessage());
            fail(scan, "DOWNLOAD_FAILED", ReceiptMessages.READ_FAILED);
            return;
        } catch (RuntimeException e) {
            log.error("Struk {} error tak terduga", scanId, e);
            fail(scan, "INTERNAL_ERROR", ReceiptMessages.READ_FAILED);
            return;
        }

        if (!result.isReceipt()) {
            fail(scan, "NOT_A_RECEIPT", ReceiptMessages.NOT_A_RECEIPT);
            return;
        }

        if (!result.usable()) {
            ReceiptScan updated = tx.execute(status -> {
                ReceiptScan s = repository.findById(scanId).orElseThrow();
                s.recordRead(result.total(), result.merchant(), result.receiptDate(), result.confident());
                s.markNeedsAmount(clock.instant());
                return repository.save(s);
            });
            InlineKeyboard skip = new InlineKeyboard(List.of(List.of(new Button("✖️ Lewati", "r:" + scanId))));
            deliver(updated, ReceiptMessages.needsAmount(result.merchant()), skip);
            return;
        }

        try {
            recordTransaction(scanId, result.total(), result);
        } catch (ApiException e) {
            fail(scan, "VALIDATION", "Gagal mencatat: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("Struk {} gagal disimpan", scanId, e);
            fail(scan, "INTERNAL_ERROR", ReceiptMessages.READ_FAILED);
        }
    }

    // ===================== 3. User mengetik nominal (NEEDS_AMOUNT) =====================

    /**
     * Jika user punya struk yang menunggu nominal dan pesannya HANYA berisi nominal (mis. "125rb"),
     * catat struk itu. Pesan lain (mis. "kopi 20rb") tidak dibajak dan diproses seperti biasa.
     */
    public Optional<TelegramWebhookReply> handleAmountReply(UUID userId, TelegramUpdate.Message msg) {
        if (msg.text() == null) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        Optional<ReceiptScan> waiting = repository.findFirstByUserIdAndStatusAndProcessedAtAfterOrderByProcessedAtDesc(
                userId, ReceiptScanStatus.NEEDS_AMOUNT, now.minus(NEEDS_AMOUNT_TTL));
        if (waiting.isEmpty()) {
            return Optional.empty();
        }
        MessageParser.Result parsed = MessageParser.parse(msg.text());
        if (!(parsed instanceof MessageParser.Entry entry) || entry.note() != null
                || entry.explicitType() == TransactionType.INCOME) {
            return Optional.empty();
        }
        ReceiptScan scan = waiting.get();
        try {
            TransactionResponse saved = recordTransaction(scan.getId(), entry.amount(), null);
            return Optional.of(TelegramWebhookReply.sendMessage(msg.chat().id(),
                    confirmationText(userId, saved, scan.getReceiptDate()), TelegramMessageFormatter.confirmationButtons(saved)));
        } catch (ApiException e) {
            return reply(msg.chat().id(), "Gagal mencatat: " + e.getMessage());
        }
    }

    /** Tombol "Lewati" pada permintaan nominal. callback_data: r:&lt;scanId&gt; */
    @Transactional
    public TelegramWebhookReply handleSkip(UUID userId, long chatId, long messageId, UUID scanId) {
        Optional<ReceiptScan> scan = repository.findByIdAndUserId(scanId, userId);
        if (scan.isPresent() && scan.get().getStatus() == ReceiptScanStatus.NEEDS_AMOUNT) {
            scan.get().markFailed("SKIPPED", clock.instant());
            repository.save(scan.get());
            return TelegramWebhookReply.editMessage(chatId, messageId, ReceiptMessages.AMOUNT_SKIPPED, null);
        }
        String text = scan.isPresent() && scan.get().getStatus() == ReceiptScanStatus.DONE
                ? "Struk ini sudah dicatat ✅"
                : ReceiptMessages.AMOUNT_SKIPPED;
        return TelegramWebhookReply.editMessage(chatId, messageId, text, null);
    }

    // ===================== 4. Pemulihan =====================

    /** Scan yang macet (server restart saat memproses) atau menunggu nominal terlalu lama. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void recoverStuck() {
        Instant now = clock.instant();
        List<ReceiptScan> stuck = repository.findByStatusInAndCreatedAtBefore(
                List.of(ReceiptScanStatus.PENDING, ReceiptScanStatus.PROCESSING), now.minus(PROCESSING_TIMEOUT));
        for (ReceiptScan scan : stuck) {
            fail(scan, "TIMEOUT", ReceiptMessages.TIMED_OUT);
        }
        List<ReceiptScan> expired = repository.findByStatusAndProcessedAtBefore(
                ReceiptScanStatus.NEEDS_AMOUNT, now.minus(NEEDS_AMOUNT_TTL));
        for (ReceiptScan scan : expired) {
            tx.executeWithoutResult(status -> repository.findById(scan.getId()).ifPresent(s -> {
                s.markFailed("AMOUNT_EXPIRED", now);
                repository.save(s);
            }));
        }
    }

    // ===================== Helper =====================

    /** Simpan transaksi dari struk, tandai DONE, lalu edit pesan "Sedang membaca…" (jika dari proses async). */
    private TransactionResponse recordTransaction(UUID scanId, BigDecimal amount, ReceiptResult result) {
        TransactionResponse saved = tx.execute(status -> {
            ReceiptScan s = repository.findById(scanId).orElseThrow();
            if (result != null) {
                s.recordRead(result.total(), result.merchant(), result.receiptDate(), result.confident());
            }
            String note = s.getCaption() != null ? s.getCaption() : s.getMerchant();
            TransactionResponse created = transactionService.createFromTelegram(
                    s.getUserId(), TransactionType.EXPENSE, transactionService.categoryIdByCode(CATEGORY_CODE),
                    amount, note, s.getChatId(), s.getMessageId(), s.getId());
            s.markDone(created.id(), amount, clock.instant());
            repository.save(s);
            return created;
        });
        if (result != null) {
            ReceiptScan s = repository.findById(scanId).orElseThrow();
            deliver(s, confirmationText(s.getUserId(), saved, s.getReceiptDate()),
                    TelegramMessageFormatter.confirmationButtons(saved));
        }
        return saved;
    }

    private String confirmationText(UUID userId, TransactionResponse saved, LocalDate receiptDate) {
        LocalDate today = transactionService.todayFor(userId);
        BigDecimal todayTotal = dashboardQueries.totalsByKind(userId, today, today).get(CategoryKind.SPENDING);
        ReceiptScanAccess access = featureAccess.receiptScan(userId);
        return TelegramMessageFormatter.confirmation(saved, todayTotal)
                + ReceiptMessages.receiptDateNote(receiptDate, today)
                + ReceiptMessages.quotaNote(access.monthlyQuota(), access.usedThisMonth());
    }

    private void fail(ReceiptScan scan, String errorCode, String userMessage) {
        ReceiptScan updated = tx.execute(status -> repository.findById(scan.getId()).map(s -> {
            s.markFailed(errorCode, clock.instant());
            return repository.save(s);
        }).orElse(scan));
        deliver(updated, userMessage, null);
    }

    /** Edit pesan "Sedang membaca…" jika ada; jika tidak, kirim pesan baru. */
    private void deliver(ReceiptScan scan, String text, InlineKeyboard keyboard) {
        if (scan.getBotMessageId() != null) {
            botClient.editMessageText(scan.getChatId(), scan.getBotMessageId(), text, keyboard);
        } else {
            botClient.sendMessage(scan.getChatId(), text, keyboard);
        }
    }

    private Optional<TelegramWebhookReply> duplicateReply(UUID userId, long chatId, ReceiptScan scan) {
        if (scan.getStatus() == ReceiptScanStatus.DONE && scan.getTransactionId() != null) {
            try {
                TransactionResponse txr = transactionService.get(userId, scan.getTransactionId());
                return reply(chatId, "Struk ini sudah dicatat sebelumnya:\n" + TelegramMessageFormatter.confirmation(txr, null));
            } catch (ApiException e) {
                return reply(chatId, "Struk ini sudah pernah dikirim (transaksinya sudah dihapus).");
            }
        }
        if (scan.getStatus() == ReceiptScanStatus.NEEDS_AMOUNT) {
            return reply(chatId, ReceiptMessages.needsAmount(scan.getMerchant()));
        }
        return reply(chatId, ReceiptMessages.ALREADY_PROCESSING);
    }

    record ImageRef(String fileId, String fileUniqueId, Long size) {}

    /** Foto terbesar, atau dokumen bergambar yang didukung. */
    static Optional<ImageRef> imageOf(TelegramUpdate.Message msg) {
        if (msg.photo() != null && !msg.photo().isEmpty()) {
            TelegramUpdate.PhotoSize largest = msg.photo().get(msg.photo().size() - 1);
            return Optional.of(new ImageRef(largest.fileId(), largest.fileUniqueId(), largest.fileSize()));
        }
        TelegramUpdate.Document doc = msg.document();
        if (doc != null && doc.mimeType() != null && SUPPORTED_MIME.contains(doc.mimeType().toLowerCase(Locale.ROOT))) {
            return Optional.of(new ImageRef(doc.fileId(), doc.fileUniqueId(), doc.fileSize()));
        }
        return Optional.empty();
    }

    /** Deteksi dari magic bytes (jangan percaya nama file / mime dari klien). */
    static String detectMediaType(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static String trimCaption(String caption) {
        if (caption == null || caption.isBlank()) {
            return null;
        }
        String c = caption.trim().replaceAll("\\s+", " ");
        return c.length() > 255 ? c.substring(0, 255) : c;
    }

    private static Optional<TelegramWebhookReply> reply(long chatId, String text) {
        return Optional.of(TelegramWebhookReply.sendMessage(chatId, text));
    }
}
