// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminAuthService.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.dto.AdminLoginRequest;
import com.example.app.modules.admin.dto.AdminResponse;
import com.example.app.modules.admin.dto.ChangePasswordRequest;
import com.example.app.modules.admin.entity.AdminUser;
import com.example.app.modules.admin.repository.AdminUserRepository;
import com.example.app.shared.config.AdminProperties;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.ratelimit.RateLimiter;
import com.example.app.shared.util.ClientContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminAuthService {

    private final AdminUserRepository repository;
    private final AdminSessionService sessionService;
    private final AdminAuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final AdminProperties props;
    private final Clock clock;
    /** Menyamakan waktu respons saat email tidak terdaftar (anti enumerasi admin). */
    private final String dummyHash;

    public AdminAuthService(AdminUserRepository repository, AdminSessionService sessionService,
                            AdminAuditService auditService, PasswordEncoder passwordEncoder, RateLimiter rateLimiter,
                            AdminProperties props, Clock clock) {
        this.repository = repository;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.props = props;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("sakutahu-admin-dummy-password-1");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AdminAuthResult login(AdminLoginRequest request, ClientContext ctx) {
        String email = AdminMapper.normalizeEmail(request.email());
        rateLimiter.consume("admin-login:" + ctx.ip() + ":" + email, 5, Duration.ofMinutes(1));
        rateLimiter.consume("admin-login-ip:" + ctx.ip(), 20, Duration.ofMinutes(10));

        Instant now = clock.instant();
        Optional<AdminUser> found = repository.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        AdminUser admin = found.get();
        if (admin.isLocked(now)) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            admin.registerFailedLogin(props.maxFailedLogins(), props.lockDuration(), now);
            throw new ApiException(admin.isLocked(now) ? ErrorCode.ACCOUNT_LOCKED : ErrorCode.INVALID_CREDENTIALS);
        }
        // Setelah password benar, agar status akun tidak bocor ke pihak yang tidak tahu password
        if (!admin.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        admin.recordLogin(now);
        return sessionService.issue(admin, ctx);
    }

    public AdminAuthResult refresh(String rawRefreshToken, ClientContext ctx) {
        rateLimiter.consume("admin-refresh:" + ctx.ip(), 30, Duration.ofMinutes(1));
        return sessionService.rotate(rawRefreshToken, ctx);
    }

    public void logout(String rawRefreshToken) {
        sessionService.revoke(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public AdminResponse me(UUID adminId) {
        return AdminMapper.toResponse(load(adminId));
    }

    /** Semua sesi lama dicabut; sesi baru dikembalikan agar admin tetap login di perangkat ini. */
    @Transactional
    public AdminAuthResult changePassword(UUID adminId, ChangePasswordRequest request, ClientContext ctx) {
        rateLimiter.consume("admin-change-password:" + adminId, 5, Duration.ofMinutes(10));
        AdminUser admin = load(adminId);
        if (!passwordEncoder.matches(request.currentPassword(), admin.getPasswordHash())) {
            throw new ApiException(ErrorCode.CURRENT_PASSWORD_INVALID);
        }
        if (passwordEncoder.matches(request.newPassword(), admin.getPasswordHash())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Password baru harus berbeda dari password saat ini");
        }
        admin.changePassword(passwordEncoder.encode(request.newPassword()));
        sessionService.revokeAll(adminId);
        auditService.record(adminId, "ADMIN_PASSWORD_CHANGED", AdminAuditService.TARGET_ADMIN, adminId, null, ctx.ip());
        return sessionService.issue(admin, ctx);
    }

    private AdminUser load(UUID adminId) {
        return repository.findById(adminId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }
}
