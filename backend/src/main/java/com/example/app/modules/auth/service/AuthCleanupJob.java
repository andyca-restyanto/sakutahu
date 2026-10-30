// filepath: /backend/src/main/java/com/example/app/modules/auth/service/AuthCleanupJob.java
package com.example.app.modules.auth.service;

import com.example.app.modules.auth.repository.EmailVerificationTokenRepository;
import com.example.app.modules.auth.repository.PasswordResetTokenRepository;
import com.example.app.modules.auth.repository.RefreshTokenRepository;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.shared.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Membersihkan token kedaluwarsa dan akun yang tidak pernah diverifikasi. */
@Component
public class AuthCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AuthCleanupJob.class);

    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AppProperties props;
    private final Clock clock;

    public AuthCleanupJob(EmailVerificationTokenRepository verificationTokenRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          UserRepository userRepository,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          AppProperties props,
                          Clock clock) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Jakarta")
    @Transactional
    public void cleanup() {
        Instant now = clock.instant();
        int tokens = verificationTokenRepository.deleteStale(now);
        // Token yang dicabut disimpan 7 hari agar deteksi reuse tetap berfungsi
        int sessions = refreshTokenRepository.deleteStale(now, now.minus(Duration.ofDays(7)));
        int resets = passwordResetTokenRepository.deleteStale(now);
        int users = userRepository.deleteUnverifiedCreatedBefore(
                now.minus(props.verification().unverifiedAccountRetention()));
        log.info("Auth cleanup: {} token verifikasi, {} refresh token, {} token reset password, "
                + "{} akun tidak terverifikasi dihapus", tokens, sessions, resets, users);
    }
}
