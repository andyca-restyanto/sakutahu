// filepath: /backend/src/main/java/com/example/app/modules/transaction/service/TransactionService.java
package com.example.app.modules.transaction.service;

import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.category.entity.Category;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.category.service.CategoryService;
import com.example.app.modules.transaction.dto.TransactionRequest;
import com.example.app.modules.transaction.dto.TransactionResponse;
import com.example.app.modules.transaction.entity.Transaction;
import com.example.app.modules.transaction.entity.TransactionSource;
import com.example.app.modules.transaction.repository.TransactionRepository;
import com.example.app.modules.transaction.repository.TransactionSpecifications;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.ratelimit.RateLimiter;
import com.example.app.shared.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class TransactionService {

    static final int MAX_PAGE_SIZE = 100;
    static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);

    private final TransactionRepository repository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public TransactionService(TransactionRepository repository,
                              CategoryService categoryService,
                              UserRepository userRepository,
                              RateLimiter rateLimiter,
                              Clock clock) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Transactional
    public TransactionResponse create(UUID userId, TransactionRequest request) {
        rateLimiter.consume("tx-write:" + userId, 60, Duration.ofMinutes(1));
        validate(userId, request);

        Transaction tx = new Transaction(userId, TransactionSource.WEB);
        tx.apply(request.type(), request.categoryId(), request.amount(), request.occurredOn(), normalizeNote(request.note()));
        return toResponse(repository.save(tx));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(UUID userId, TransactionType type, Long categoryId,
                                                  LocalDate from, LocalDate to, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tanggal awal tidak boleh setelah tanggal akhir");
        }
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Order.desc("occurredOn"), Sort.Order.desc("createdAt")));
        var result = repository.findAll(TransactionSpecifications.filter(userId, type, categoryId, from, to), pageable);
        return PageResponse.of(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID userId, UUID id) {
        return toResponse(findOwned(userId, id));
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID id, TransactionRequest request) {
        rateLimiter.consume("tx-write:" + userId, 60, Duration.ofMinutes(1));
        Transaction tx = findOwned(userId, id);
        validate(userId, request);
        tx.apply(request.type(), request.categoryId(), request.amount(), request.occurredOn(), normalizeNote(request.note()));
        return toResponse(repository.save(tx));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repository.delete(findOwned(userId, id));
    }

    // ---------- Dipakai oleh modul telegram ----------

    /**
     * Mencatat transaksi dari pesan Telegram, selalu untuk HARI INI menurut zona waktu user.
     * Idempoten: pesan yang sama (chat + message id) tidak pernah tercatat dua kali.
     */
    @Transactional
    public TransactionResponse createFromTelegram(UUID userId, TransactionType type, Long categoryId,
                                                  BigDecimal amount, String note, long chatId, long messageId) {
        return createFromTelegram(userId, type, categoryId, amount, note, chatId, messageId, null);
    }

    /** Varian untuk struk: receiptScanId menandai transaksi berasal dari foto struk. */
    @Transactional
    public TransactionResponse createFromTelegram(UUID userId, TransactionType type, Long categoryId,
                                                  BigDecimal amount, String note, long chatId, long messageId,
                                                  UUID receiptScanId) {
        var existing = repository.findByTelegramChatIdAndTelegramMessageId(chatId, messageId);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        TransactionRequest request = new TransactionRequest(type, categoryId, amount, todayFor(userId), note);
        validate(userId, request);
        Transaction tx = Transaction.fromTelegram(userId, chatId, messageId, receiptScanId);
        tx.apply(type, categoryId, amount, request.occurredOn(), normalizeNote(note));
        return toResponse(repository.save(tx));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<TransactionResponse> findByTelegramMessage(long chatId, long messageId) {
        return repository.findByTelegramChatIdAndTelegramMessageId(chatId, messageId).map(this::toResponse);
    }

    /** Ganti kategori saja (jenis transaksi tetap). */
    @Transactional
    public TransactionResponse changeCategory(UUID userId, UUID id, Long categoryId) {
        Transaction tx = findOwned(userId, id);
        Category category = categoryService.findActive(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "Kategori tidak ditemukan"));
        if (category.getType() != tx.getType()) {
            throw new ApiException(ErrorCode.CATEGORY_TYPE_MISMATCH);
        }
        tx.changeCategory(categoryId);
        return toResponse(repository.save(tx));
    }

    /** Transaksi Telegram terakhir milik user hari ini (untuk perintah /batal). */
    @Transactional(readOnly = true)
    public java.util.Optional<TransactionResponse> lastTelegramToday(UUID userId) {
        return repository.findFirstByUserIdAndSourceAndOccurredOnOrderByCreatedAtDesc(
                userId, TransactionSource.TELEGRAM, todayFor(userId)).map(this::toResponse);
    }

    /** Id kategori bawaan berdasarkan code (mis. PERSONAL_SHOPPING untuk struk). */
    public Long categoryIdByCode(String code) {
        return categoryService.list(null).stream()
                .filter(c -> c.code().equals(code))
                .map(c -> c.id())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Kategori " + code + " tidak ada"));
    }

    /** Tanggal hari ini menurut zona waktu user. */
    @Transactional(readOnly = true)
    public LocalDate todayFor(UUID userId) {
        return LocalDate.now(clock.withZone(userZone(userId)));
    }

    /** 404 juga untuk transaksi milik user lain, agar keberadaannya tidak bocor. */
    private Transaction findOwned(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    private void validate(UUID userId, TransactionRequest request) {
        Category category = categoryService.findActive(request.categoryId())
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "Kategori tidak ditemukan"));
        if (category.getType() != request.type()) {
            throw new ApiException(ErrorCode.CATEGORY_TYPE_MISMATCH);
        }

        LocalDate today = LocalDate.now(clock.withZone(userZone(userId)));
        if (request.occurredOn().isAfter(today)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tanggal transaksi tidak boleh di masa depan");
        }
        if (request.occurredOn().isBefore(MIN_DATE)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tanggal transaksi terlalu lama");
        }
    }

    private ZoneId userZone(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> ZoneId.of(u.getTimezone()))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }

    private static String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType(),
                categoryService.summaryOf(tx.getCategoryId()),
                tx.getAmount(),
                tx.getOccurredOn(),
                tx.getNote(),
                tx.getSource(),
                tx.getReceiptScanId(),
                tx.getCreatedAt());
    }
}
