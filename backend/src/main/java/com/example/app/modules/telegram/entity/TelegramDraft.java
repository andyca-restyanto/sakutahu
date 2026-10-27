// filepath: /backend/src/main/java/com/example/app/modules/telegram/entity/TelegramDraft.java
package com.example.app.modules.telegram.entity;

import com.example.app.modules.category.entity.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Pesan yang kategorinya belum tertebak, menunggu user memilih kategori lewat tombol. */
@Entity
@Table(name = "telegram_drafts")
public class TelegramDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "source_message_id", nullable = false)
    private Long sourceMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected TelegramDraft() {}

    public TelegramDraft(UUID userId, long chatId, long sourceMessageId, TransactionType type, BigDecimal amount,
                         String note, LocalDate occurredOn, Instant expiresAt) {
        this.userId = userId;
        this.chatId = chatId;
        this.sourceMessageId = sourceMessageId;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.occurredOn = occurredOn;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(Instant now, LocalDate today) {
        return expiresAt.isAfter(now) && occurredOn.equals(today);
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public Long getChatId() { return chatId; }
    public Long getSourceMessageId() { return sourceMessageId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getNote() { return note; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public Instant getExpiresAt() { return expiresAt; }
}
