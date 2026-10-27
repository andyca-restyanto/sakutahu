// filepath: /backend/src/main/java/com/example/app/modules/telegram/entity/TelegramAccount.java
package com.example.app.modules.telegram.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telegram_accounts")
public class TelegramAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Referensi ke users.id (tanpa relasi JPA agar modul tetap terisolasi). */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "telegram_user_id", nullable = false)
    private Long telegramUserId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(length = 64)
    private String username;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    protected TelegramAccount() {}

    public TelegramAccount(UUID userId) {
        this.userId = userId;
    }

    public void link(long telegramUserId, long chatId, String username, Instant now) {
        this.telegramUserId = telegramUserId;
        this.chatId = chatId;
        this.username = username;
        this.linkedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Long getTelegramUserId() { return telegramUserId; }
    public Long getChatId() { return chatId; }
    public String getUsername() { return username; }
    public Instant getLinkedAt() { return linkedAt; }
}
