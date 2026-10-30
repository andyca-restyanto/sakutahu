// filepath: /backend/src/main/java/com/example/app/modules/auth/entity/User.java
package com.example.app.modules.auth.entity;

import com.example.app.modules.subscription.entity.UserPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Selalu disimpan dalam huruf kecil. */
    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    /** Paket tersimpan; gunakan PlanService.effectivePlan() yang memperhitungkan masa berlaku. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserPlan plan = UserPlan.FREE;

    /** Null = VIP tanpa batas waktu (atau tidak relevan untuk FREE). */
    @Column(name = "plan_expires_at")
    private Instant planExpiresAt;

    /** Diisi admin saat memblokir; null = akun aktif. */
    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "blocked_reason", length = 255)
    private String blockedReason;

    @Column(name = "blocked_by")
    private UUID blockedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {}

    public User(String email, String passwordHash, String fullName, String currency, String timezone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.currency = currency;
        this.timezone = timezone;
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

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void markEmailVerified(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }

    /** Menambah hitungan gagal; mengunci akun jika melewati batas. */
    public void registerFailedLogin(int maxFailed, Duration lockDuration, Instant now) {
        failedLoginCount++;
        if (failedLoginCount >= maxFailed) {
            lockedUntil = now.plus(lockDuration);
            failedLoginCount = 0;
        }
    }

    /** Password baru juga membuka kunci akun akibat salah password. */
    public void changePassword(String newHash) {
        this.passwordHash = newHash;
        resetFailedLogins();
    }

    public boolean isBlocked() {
        return blockedAt != null;
    }

    /** Diubah oleh admin. FREE selalu tanpa tanggal kedaluwarsa. */
    public void changePlan(UserPlan newPlan, Instant expiresAt) {
        this.plan = newPlan;
        this.planExpiresAt = newPlan == UserPlan.VIP ? expiresAt : null;
    }

    public void block(String reason, UUID adminId, Instant now) {
        this.blockedAt = now;
        this.blockedReason = reason;
        this.blockedBy = adminId;
    }

    public void unblock() {
        this.blockedAt = null;
        this.blockedReason = null;
        this.blockedBy = null;
    }

    public void resetFailedLogins() {
        failedLoginCount = 0;
        lockedUntil = null;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getCurrency() { return currency; }
    public String getTimezone() { return timezone; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public UserPlan getPlan() { return plan; }
    public Instant getPlanExpiresAt() { return planExpiresAt; }
    public Instant getBlockedAt() { return blockedAt; }
    public String getBlockedReason() { return blockedReason; }
    public UUID getBlockedBy() { return blockedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
