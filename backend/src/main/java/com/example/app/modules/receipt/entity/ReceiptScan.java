// filepath: /backend/src/main/java/com/example/app/modules/receipt/entity/ReceiptScan.java
package com.example.app.modules.receipt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Satu foto struk yang dikirim user. Foto tidak disimpan; hanya referensi Telegram & hasil baca. */
@Entity
@Table(name = "receipt_scans")
public class ReceiptScan implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "chat_id", nullable = false, updatable = false)
    private Long chatId;

    /** message_id foto dari user; dipakai juga sebagai kunci idempoten transaksi. */
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    /** Pesan bot "Sedang membaca struk…" yang nanti diedit menjadi hasil. */
    @Column(name = "bot_message_id")
    private Long botMessageId;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "file_unique_id", nullable = false, updatable = false)
    private String fileUniqueId;

    @Column(length = 255)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReceiptScanStatus status;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 120)
    private String merchant;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(length = 10)
    private String confidence;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "error_code", length = 40)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Transient
    private boolean isNew = true;

    protected ReceiptScan() {}

    public ReceiptScan(UUID userId, long chatId, long messageId, String fileId, String fileUniqueId, String caption,
                       Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.chatId = chatId;
        this.messageId = messageId;
        this.fileId = fileId;
        this.fileUniqueId = fileUniqueId;
        this.caption = caption;
        this.status = ReceiptScanStatus.PENDING;
        this.createdAt = now;
    }

    /** Foto yang sama dikirim ulang setelah sebelumnya gagal: proses lagi dari awal. */
    public void retry(long messageId, String fileId, String caption, Instant now) {
        this.messageId = messageId;
        this.fileId = fileId;
        this.caption = caption;
        this.status = ReceiptScanStatus.PENDING;
        this.amount = null;
        this.merchant = null;
        this.receiptDate = null;
        this.confidence = null;
        this.errorCode = null;
        this.botMessageId = null;
        this.processedAt = null;
        this.createdAt = now;
    }

    public void attachBotMessage(Long botMessageId) {
        this.botMessageId = botMessageId;
    }

    public void markProcessing() {
        this.status = ReceiptScanStatus.PROCESSING;
    }

    public void recordRead(BigDecimal amount, String merchant, LocalDate receiptDate, boolean confident) {
        this.amount = amount;
        this.merchant = merchant;
        this.receiptDate = receiptDate;
        this.confidence = confident ? "high" : "low";
    }

    public void markDone(UUID transactionId, BigDecimal finalAmount, Instant now) {
        this.status = ReceiptScanStatus.DONE;
        this.transactionId = transactionId;
        this.amount = finalAmount;
        this.processedAt = now;
    }

    public void markNeedsAmount(Instant now) {
        this.status = ReceiptScanStatus.NEEDS_AMOUNT;
        this.processedAt = now;
    }

    public void markFailed(String errorCode, Instant now) {
        this.status = ReceiptScanStatus.FAILED;
        this.errorCode = errorCode;
        this.processedAt = now;
    }

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
    public Long getChatId() { return chatId; }
    public Long getMessageId() { return messageId; }
    public Long getBotMessageId() { return botMessageId; }
    public String getFileId() { return fileId; }
    public String getFileUniqueId() { return fileUniqueId; }
    public String getCaption() { return caption; }
    public ReceiptScanStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getMerchant() { return merchant; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public UUID getTransactionId() { return transactionId; }
    public String getErrorCode() { return errorCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}
