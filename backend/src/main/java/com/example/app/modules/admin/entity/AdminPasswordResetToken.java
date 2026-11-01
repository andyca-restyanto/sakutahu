// filepath: /backend/src/main/java/com/example/app/modules/admin/entity/AdminPasswordResetToken.java
package com.example.app.modules.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Token link "lupa password" admin. Token mentah hanya ada di email; database menyimpan hash-nya. */
@Entity
@Table(name = "admin_password_reset_tokens")
public class AdminPasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(length = 45)
    private String ip;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AdminPasswordResetToken() {}

    public AdminPasswordResetToken(UUID adminId, String tokenHash, Instant expiresAt, String ip, UUID requestedBy) {
        this.adminId = adminId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.ip = ip;
        this.requestedBy = requestedBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) {
        usedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getAdminId() { return adminId; }
    public UUID getRequestedBy() { return requestedBy; }
    public Instant getExpiresAt() { return expiresAt; }
}
