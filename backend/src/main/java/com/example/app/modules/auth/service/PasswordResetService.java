// filepath: /backend/src/main/java/com/example/app/modules/auth/service/PasswordResetService.java
package com.example.app.modules.auth.service;

import com.example.app.modules.auth.dto.MessageResponse;
import com.example.app.modules.auth.dto.ResetPasswordInfoResponse;
import com.example.app.modules.auth.entity.PasswordResetToken;
import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.PasswordResetTokenRepository;
import com.example.app.modules.auth.repository.RefreshTokenRepository;
import com.example.app.modules.auth.repository.UserRepository;
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
import java.util.Locale;

/**
 * Alur "lupa password":
 * 1. forgot  → selalu membalas pesan yang sama (tidak membocorkan email terdaftar atau tidak).
 * 2. inspect → halaman reset memeriksa link sebelum menampilkan form.
 * 3. reset   → password baru, link dipakai sekali, semua sesi di semua perangkat dicabut.
 */
@Service
public class PasswordResetService {

    static final String MSG_FORGOT = "Jika email terdaftar, link untuk mengatur ulang password telah dikirim";
    static final String MSG_RESET = "Password berhasil diganti. Silakan masuk dengan password baru.";

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final RateLimiter rateLimiter;
    private final Clock clock;
    private final Duration tokenTtl;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository, UserRepository userRepository,
                                RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                                ApplicationEventPublisher events, RateLimiter rateLimiter, Clock clock,
                                @Value("${app.password-reset.token-ttl:PT15M}") Duration tokenTtl) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
    }

    @Transactional
    public MessageResponse requestReset(String rawEmail, ClientContext ctx) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        // Berlaku sama untuk email terdaftar maupun tidak
        rateLimiter.consume("forgot:" + email, 1, Duration.ofMinutes(1));
        rateLimiter.consume("forgot-hourly:" + email, 5, Duration.ofHours(1));
        rateLimiter.consume("forgot-ip:" + ctx.ip(), 20, Duration.ofHours(1));

        // User diblokir tetap mendapat pesan yang sama, tapi email tidak dikirim
        userRepository.findByEmail(email)
                .filter(u -> !u.isBlocked())
                .ifPresent(user -> {
                    Instant now = clock.instant();
                    tokenRepository.invalidateActiveForUser(user.getId(), now);
                    String raw = TokenUtils.randomToken();
                    tokenRepository.save(new PasswordResetToken(user.getId(), TokenUtils.sha256Hex(raw),
                            now.plus(tokenTtl), ctx.ip()));
                    events.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), user.getFullName(), raw, tokenTtl));
                });
        return new MessageResponse(MSG_FORGOT);
    }

    @Transactional(readOnly = true)
    public ResetPasswordInfoResponse inspect(String rawToken, ClientContext ctx) {
        rateLimiter.consume("reset-inspect:" + ctx.ip(), 30, Duration.ofMinutes(10));
        PasswordResetToken token = validToken(rawToken);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_TOKEN_INVALID));
        return new ResetPasswordInfoResponse(maskEmail(user.getEmail()), token.getExpiresAt());
    }

    @Transactional
    public MessageResponse reset(String rawToken, String newPassword, ClientContext ctx) {
        rateLimiter.consume("reset:" + ctx.ip(), 10, Duration.ofMinutes(10));
        PasswordResetToken token = validToken(rawToken);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_TOKEN_INVALID));
        if (user.isBlocked()) {
            throw new ApiException(ErrorCode.ACCOUNT_BLOCKED);
        }

        Instant now = clock.instant();
        token.markUsed(now);
        tokenRepository.invalidateActiveForUser(user.getId(), now);
        user.changePassword(passwordEncoder.encode(newPassword));
        // Pemilik link terbukti memegang email ini
        user.markEmailVerified(now);
        userRepository.save(user);
        // Logout dari semua perangkat; user login ulang dengan password baru
        refreshTokenRepository.revokeAllActiveForUser(user.getId(), now);

        events.publishEvent(new PasswordChangedEvent(user.getEmail(), user.getFullName(), user.getTimezone(), now, true));
        return new MessageResponse(MSG_RESET);
    }

    private PasswordResetToken validToken(String rawToken) {
        PasswordResetToken token = tokenRepository.findByTokenHash(TokenUtils.sha256Hex(rawToken.trim()))
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_TOKEN_INVALID));
        if (token.isUsed()) {
            throw new ApiException(ErrorCode.RESET_TOKEN_INVALID);
        }
        if (token.isExpired(clock.instant())) {
            throw new ApiException(ErrorCode.RESET_TOKEN_EXPIRED);
        }
        return token;
    }

    /** budi@mail.com → bu***@mail.com */
    static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }
}
