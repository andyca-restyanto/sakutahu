// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminSessionService.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.dto.AdminAuthResponse;
import com.example.app.modules.admin.entity.AdminRefreshToken;
import com.example.app.modules.admin.entity.AdminUser;
import com.example.app.modules.admin.repository.AdminRefreshTokenRepository;
import com.example.app.modules.admin.repository.AdminUserRepository;
import com.example.app.shared.config.AdminProperties;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.security.AdminJwtService;
import com.example.app.shared.util.ClientContext;
import com.example.app.shared.util.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Sesi admin: access token 10 menit + refresh token 8 jam (dirotasi, pemakaian ulang mencabut semua sesi). */
@Service
public class AdminSessionService {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionService.class);

    private final AdminRefreshTokenRepository tokenRepository;
    private final AdminUserRepository adminRepository;
    private final AdminJwtService jwtService;
    private final AdminProperties props;
    private final Clock clock;

    public AdminSessionService(AdminRefreshTokenRepository tokenRepository, AdminUserRepository adminRepository,
                               AdminJwtService jwtService, AdminProperties props, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.adminRepository = adminRepository;
        this.jwtService = jwtService;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public AdminAuthResult issue(AdminUser admin, ClientContext ctx) {
        String raw = TokenUtils.randomToken();
        tokenRepository.save(newToken(admin.getId(), raw, ctx));
        return result(admin, raw);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AdminAuthResult rotate(String rawToken, ClientContext ctx) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        }
        Instant now = clock.instant();
        AdminRefreshToken current = tokenRepository.findByTokenHash(TokenUtils.sha256Hex(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_EXPIRED));
        if (current.isRevoked()) {
            // Sama seperti sesi user: hanya token hasil rotasi yang dipakai ulang yang dianggap pencurian
            if (current.getReplacedBy() != null) {
                log.warn("Refresh token admin dipakai ulang untuk admin {}, semua sesi dicabut", current.getAdminId());
                tokenRepository.revokeAllActive(current.getAdminId(), now);
            }
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        }
        if (current.isExpired(now)) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        }
        AdminUser admin = adminRepository.findById(current.getAdminId())
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_EXPIRED));
        if (!admin.isActive()) {
            tokenRepository.revokeAllActive(admin.getId(), now);
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }

        String raw = TokenUtils.randomToken();
        AdminRefreshToken next = newToken(admin.getId(), raw, ctx);
        tokenRepository.save(next);
        current.revoke(now, next.getId());
        tokenRepository.save(current);
        return result(admin, raw);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        tokenRepository.findByTokenHash(TokenUtils.sha256Hex(rawToken))
                .filter(t -> !t.isRevoked())
                .ifPresent(t -> t.revoke(clock.instant(), null));
    }

    @Transactional
    public void revokeAll(UUID adminId) {
        tokenRepository.revokeAllActive(adminId, clock.instant());
    }

    private AdminRefreshToken newToken(UUID adminId, String raw, ClientContext ctx) {
        return new AdminRefreshToken(adminId, TokenUtils.sha256Hex(raw),
                clock.instant().plus(props.refreshTokenTtl()), ctx.userAgent(), ctx.ip());
    }

    private AdminAuthResult result(AdminUser admin, String rawRefresh) {
        String access = jwtService.generateAccessToken(admin.getId(), admin.getEmail());
        return new AdminAuthResult(new AdminAuthResponse(access, "Bearer", jwtService.accessTokenTtlSeconds(),
                AdminMapper.toResponse(admin)), rawRefresh);
    }
}
