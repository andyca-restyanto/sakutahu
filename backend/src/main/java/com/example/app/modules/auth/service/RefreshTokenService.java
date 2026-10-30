// filepath: /backend/src/main/java/com/example/app/modules/auth/service/RefreshTokenService.java
package com.example.app.modules.auth.service;

import com.example.app.modules.auth.dto.AuthResponse;
import com.example.app.modules.auth.entity.RefreshToken;
import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.RefreshTokenRepository;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.shared.config.AppProperties;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.security.JwtService;
import com.example.app.shared.util.ClientContext;
import com.example.app.shared.util.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Membuat sesi (access + refresh token), rotasi, dan pencabutan. */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserResponseMapper userResponseMapper;
    private final AppProperties props;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               JwtService jwtService,
                               UserResponseMapper userResponseMapper,
                               AppProperties props,
                               Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userResponseMapper = userResponseMapper;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public AuthResult issue(User user, ClientContext ctx) {
        if (user.isBlocked()) {
            throw new ApiException(ErrorCode.ACCOUNT_BLOCKED);
        }
        String raw = TokenUtils.randomToken();
        RefreshToken token = newToken(user, raw, ctx);
        refreshTokenRepository.save(token);
        return buildResult(user, raw);
    }

    /**
     * Rotasi refresh token. Jika token yang sudah dicabut dipakai ulang (indikasi pencurian),
     * semua sesi user tersebut dicabut.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult rotate(String rawToken, ClientContext ctx) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        }
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository.findByTokenHash(TokenUtils.sha256Hex(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_EXPIRED));

        if (current.isRevoked()) {
            log.warn("Refresh token reuse terdeteksi untuk user {}, semua sesi dicabut", current.getUserId());
            refreshTokenRepository.revokeAllActiveForUser(current.getUserId(), now);
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        }
        if (current.isExpired(now)) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_EXPIRED));
        if (user.isLocked(now)) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.isBlocked()) {
            refreshTokenRepository.revokeAllActiveForUser(user.getId(), now);
            throw new ApiException(ErrorCode.ACCOUNT_BLOCKED);
        }

        String raw = TokenUtils.randomToken();
        RefreshToken next = newToken(user, raw, ctx);
        refreshTokenRepository.save(next);
        current.revoke(now, next.getId());
        refreshTokenRepository.save(current);

        return buildResult(user, raw);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(TokenUtils.sha256Hex(rawToken))
                .filter(t -> !t.isRevoked())
                .ifPresent(t -> {
                    t.revoke(clock.instant(), null);
                    refreshTokenRepository.save(t);
                });
    }

    /** Mencabut semua sesi user (dipakai saat ganti/reset password). */
    @Transactional
    public void revokeAll(UUID userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId, clock.instant());
    }

    private RefreshToken newToken(User user, String raw, ClientContext ctx) {
        Instant expiresAt = clock.instant().plus(props.jwt().refreshTokenTtl());
        return new RefreshToken(user.getId(), TokenUtils.sha256Hex(raw), expiresAt, ctx.userAgent(), ctx.ip());
    }

    private AuthResult buildResult(User user, String rawRefresh) {
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
        AuthResponse response = new AuthResponse(access, "Bearer", jwtService.accessTokenTtlSeconds(),
                userResponseMapper.toResponse(user));
        return new AuthResult(response, rawRefresh);
    }
}
