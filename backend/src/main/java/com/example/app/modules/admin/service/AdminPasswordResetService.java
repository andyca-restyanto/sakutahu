// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminPasswordResetService.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.dto.AdminResetPasswordInfoResponse;
import com.example.app.modules.admin.entity.AdminPasswordResetToken;
import com.example.app.modules.admin.entity.AdminUser;
import com.example.app.modules.admin.repository.AdminPasswordResetTokenRepository;
import com.example.app.modules.admin.repository.AdminUserRepository;
import com.example.app.modules.auth.dto.MessageResponse;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.ratelimit.RateLimiter;
import com.example.app.shared.util.ClientContext;
import com.example.app.shared.util.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Lupa password admin/OWNER. Terpisah total dari reset password user (tabel, endpoint, dan halaman).
 * - requestReset: diminta sendiri dari /admin/forgot-password (balasan selalu sama).
 * - sendByOwner : OWNER mengirim link ke admin lain dari menu Admin (OWNER tidak pernah melihat password).
 * - reset       : password baru, semua sesi admin dicabut, login ulang.
 */
@Service
public class AdminPasswordResetService {

    static final String MSG_FORGOT = "Jika email terdaftar sebagai admin, link untuk mengatur ulang password telah dikirim";
    static final String MSG_RESET = "Password admin berhasil diganti. Silakan masuk dengan password baru.";

    private final AdminPasswordResetTokenRepository tokenRepository;
    private final AdminUserRepository adminRepository;
    private final AdminSessionService sessionService;
    private final AdminAuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final RateLimiter rateLimiter;
    private final Clock clock;
    private final Duration tokenTtl;

    public AdminPasswordResetService(AdminPasswordResetTokenRepository tokenRepository,
                                     AdminUserRepository adminRepository, AdminSessionService sessionService,
                                     AdminAuditService auditService, PasswordEncoder passwordEncoder,
                                     ApplicationEventPublisher events, RateLimiter rateLimiter, Clock clock,
                                     @Value("${app.admin.password-reset-ttl:PT15M}") Duration tokenTtl) {
        this.tokenRepository = tokenRepository;
        this.adminRepository = adminRepository;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
    }

    @Transactional
    public MessageResponse requestReset(String rawEmail, ClientContext ctx) {
        String email = AdminMapper.normalizeEmail(rawEmail);
        rateLimiter.consume("admin-forgot:" + email, 1, Duration.ofMinutes(1));
        rateLimiter.consume("admin-forgot-hourly:" + email, 3, Duration.ofHours(1));
        rateLimiter.consume("admin-forgot-ip:" + ctx.ip(), 10, Duration.ofHours(1));

        // Admin DISABLED tetap mendapat pesan yang sama, tapi email tidak dikirim
        adminRepository.findByEmail(email).filter(AdminUser::isActive).ifPresent(admin -> {
            issue(admin, null, null, ctx);
            auditService.record(admin.getId(), "ADMIN_PASSWORD_RESET_REQUESTED", AdminAuditService.TARGET_ADMIN,
                    admin.getId(), AdminAuditService.detail("by", "self"), ctx.ip());
        });
        return new MessageResponse(MSG_FORGOT);
    }

    /** Hanya OWNER (dijaga SecurityConfig). Target OWNER sendiri memakai alur lupa password biasa. */
    @Transactional
    public MessageResponse sendByOwner(UUID ownerId, UUID adminId, ClientContext ctx) {
        rateLimiter.consume("admin-reset-by-owner:" + adminId, 3, Duration.ofHours(1));
        AdminUser target = adminRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Admin tidak ditemukan"));
        if (target.isOwner()) {
            throw new ApiException(ErrorCode.ADMIN_OWNER_PROTECTED,
                    "Untuk akun OWNER, gunakan \"Lupa password?\" di halaman login admin");
        }
        if (!target.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED, "Aktifkan admin ini terlebih dahulu");
        }
        AdminUser owner = adminRepository.findById(ownerId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        issue(target, ownerId, owner.getFullName(), ctx);
        auditService.record(ownerId, "ADMIN_PASSWORD_RESET_REQUESTED", AdminAuditService.TARGET_ADMIN, adminId,
                AdminAuditService.detail("by", "owner", "email", target.getEmail()), ctx.ip());
        return new MessageResponse("Link reset password dikirim ke " + maskEmail(target.getEmail()));
    }

    @Transactional(readOnly = true)
    public AdminResetPasswordInfoResponse inspect(String rawToken, ClientContext ctx) {
        rateLimiter.consume("admin-reset-inspect:" + ctx.ip(), 30, Duration.ofMinutes(10));
        AdminPasswordResetToken token = validToken(rawToken);
        AdminUser admin = adminRepository.findById(token.getAdminId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_TOKEN_INVALID));
        return new AdminResetPasswordInfoResponse(maskEmail(admin.getEmail()), token.getExpiresAt());
    }

    @Transactional
    public MessageResponse reset(String rawToken, String newPassword, ClientContext ctx) {
        rateLimiter.consume("admin-reset:" + ctx.ip(), 10, Duration.ofMinutes(10));
        AdminPasswordResetToken token = validToken(rawToken);
        AdminUser admin = adminRepository.findById(token.getAdminId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_TOKEN_INVALID));
        if (!admin.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }

        Instant now = clock.instant();
        token.markUsed(now);
        tokenRepository.invalidateActive(admin.getId(), now);
        admin.resetPassword(passwordEncoder.encode(newPassword));
        // Logout dari semua perangkat; admin masuk ulang dengan password baru
        sessionService.revokeAll(admin.getId());
        auditService.record(admin.getId(), "ADMIN_PASSWORD_RESET", AdminAuditService.TARGET_ADMIN, admin.getId(),
                AdminAuditService.detail("via", token.getRequestedBy() == null ? "self" : "owner"), ctx.ip());
        events.publishEvent(new AdminPasswordEmailEvents.PasswordReset(admin.getEmail(), admin.getFullName(), now));
        return new MessageResponse(MSG_RESET);
    }

    private void issue(AdminUser admin, UUID requestedBy, String ownerName, ClientContext ctx) {
        Instant now = clock.instant();
        tokenRepository.invalidateActive(admin.getId(), now);
        String raw = TokenUtils.randomToken();
        tokenRepository.save(new AdminPasswordResetToken(admin.getId(), TokenUtils.sha256Hex(raw),
                now.plus(tokenTtl), ctx.ip(), requestedBy));
        events.publishEvent(new AdminPasswordEmailEvents.ResetRequested(admin.getEmail(), admin.getFullName(), raw,
                tokenTtl, ownerName));
    }

    private AdminPasswordResetToken validToken(String rawToken) {
        AdminPasswordResetToken token = tokenRepository.findByTokenHash(TokenUtils.sha256Hex(rawToken.trim()))
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_TOKEN_INVALID));
        if (token.isUsed()) {
            throw new ApiException(ErrorCode.RESET_TOKEN_INVALID);
        }
        if (token.isExpired(clock.instant())) {
            throw new ApiException(ErrorCode.RESET_TOKEN_EXPIRED);
        }
        return token;
    }

    static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }
}
