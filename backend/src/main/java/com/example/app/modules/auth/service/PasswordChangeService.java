// filepath: /backend/src/main/java/com/example/app/modules/auth/service/PasswordChangeService.java
package com.example.app.modules.auth.service;

import com.example.app.modules.auth.dto.UpdatePasswordRequest;
import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.ratelimit.RateLimiter;
import com.example.app.shared.util.ClientContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Ganti password oleh user yang sedang login. Perangkat lain otomatis logout; perangkat ini dapat sesi baru. */
@Service
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public PasswordChangeService(UserRepository userRepository, RefreshTokenService refreshTokenService,
                                 PasswordEncoder passwordEncoder, ApplicationEventPublisher events,
                                 RateLimiter rateLimiter, Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Transactional
    public AuthResult change(UUID userId, UpdatePasswordRequest request, ClientContext ctx) {
        rateLimiter.consume("change-password:" + userId, 5, Duration.ofMinutes(10));
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.CURRENT_PASSWORD_INVALID);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Password baru harus berbeda dari password saat ini");
        }

        Instant now = clock.instant();
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAll(userId);
        events.publishEvent(new PasswordChangedEvent(user.getEmail(), user.getFullName(), user.getTimezone(), now, false));
        // Sesi baru untuk perangkat ini (cookie refresh lama ikut dicabut di atas)
        return refreshTokenService.issue(user, ctx);
    }
}
