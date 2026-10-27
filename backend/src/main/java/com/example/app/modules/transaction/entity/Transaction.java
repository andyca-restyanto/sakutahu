// filepath: /backend/src/main/java/com/example/app/modules/transaction/entity/Transaction.java
package com.example.app.modules.transaction.entity;

import com.example.app.modules.category.entity.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    /** Referensi ke categories.id (tanpa relasi JPA; info kategori diambil dari cache CategoryService). */
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Tanggal lokal menurut user (bukan timestamp), agar pengelompokan harian/bulanan tidak bergeser zona waktu. */
    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(length = 255)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TransactionSource source;

    /** Diisi untuk transaksi dari Telegram: pasangan unik untuk mencegah pencatatan ganda. */
    @Column(name = "telegram_chat_id", updatable = false)
    private Long telegramChatId;

    @Column(name = "telegram_message_id", updatable = false)
    private Long telegramMessageId;

    /** Diisi jika transaksi dicatat dari foto struk (modul receipt). */
    @Column(name = "receipt_scan_id", updatable = false)
    private UUID receiptScanId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean isNew = true;

    protected Transaction() {}

    public Transaction(UUID userId, TransactionSource source) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.source = source;
    }

    public static Transaction fromTelegram(UUID userId, long chatId, long messageId) {
        return fromTelegram(userId, chatId, messageId, null);
    }

    public static Transaction fromTelegram(UUID userId, long chatId, long messageId, UUID receiptScanId) {
        Transaction tx = new Transaction(userId, TransactionSource.TELEGRAM);
        tx.telegramChatId = chatId;
        tx.telegramMessageId = messageId;
        tx.receiptScanId = receiptScanId;
        return tx;
    }

    public void changeCategory(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void apply(TransactionType type, Long categoryId, BigDecimal amount, LocalDate occurredOn, String note) {
        this.type = type;
        this.categoryId = categoryId;
        this.amount = amount;
        this.occurredOn = occurredOn;
        this.note = note;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Id dibuat di aplikasi; Persistable mencegah Spring Data melakukan SELECT sebelum INSERT. */
    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public TransactionType getType() { return type; }
    public Long getCategoryId() { return categoryId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public String getNote() { return note; }
    public TransactionSource getSource() { return source; }
    public Long getTelegramChatId() { return telegramChatId; }
    public Long getTelegramMessageId() { return telegramMessageId; }
    public UUID getReceiptScanId() { return receiptScanId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
