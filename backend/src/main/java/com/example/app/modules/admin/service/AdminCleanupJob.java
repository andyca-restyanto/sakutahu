// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminCleanupJob.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.repository.AdminInvitationRepository;
import com.example.app.modules.admin.repository.AdminRefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Membersihkan sesi admin lama & undangan kedaluwarsa setiap malam. */
@Component
public class AdminCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AdminCleanupJob.class);

    private final AdminRefreshTokenRepository tokenRepository;
    private final AdminInvitationRepository invitationRepository;
    private final Clock clock;

    public AdminCleanupJob(AdminRefreshTokenRepository tokenRepository, AdminInvitationRepository invitationRepository,
                           Clock clock) {
        this.tokenRepository = tokenRepository;
        this.invitationRepository = invitationRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 45 3 * * *", zone = "Asia/Jakarta")
    @Transactional
    public void cleanup() {
        Instant now = clock.instant();
        int tokens = tokenRepository.deleteStale(now, now.minus(Duration.ofDays(7)));
        int invitations = invitationRepository.deleteExpiredBefore(now.minus(Duration.ofDays(30)));
        log.info("Admin cleanup: {} refresh token, {} undangan dihapus", tokens, invitations);
    }
}
