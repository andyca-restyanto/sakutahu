// filepath: /backend/src/main/java/com/example/app/modules/telegram/service/TelegramCleanupJob.java
package com.example.app.modules.telegram.service;

import com.example.app.modules.telegram.repository.TelegramDraftRepository;
import com.example.app.modules.telegram.repository.TelegramLinkTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class TelegramCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TelegramCleanupJob.class);

    private final TelegramLinkTokenRepository tokenRepository;
    private final TelegramDraftRepository draftRepository;
    private final Clock clock;

    public TelegramCleanupJob(TelegramLinkTokenRepository tokenRepository,
                              TelegramDraftRepository draftRepository,
                              Clock clock) {
        this.tokenRepository = tokenRepository;
        this.draftRepository = draftRepository;
        this.clock = clock;
    }

    /** Draft yang tidak dipilih kategorinya dalam 15 menit dibuang. */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void cleanupDrafts() {
        int deleted = draftRepository.deleteExpired(clock.instant());
        if (deleted > 0) {
            log.debug("Telegram cleanup: {} draft kedaluwarsa dihapus", deleted);
        }
    }

    @Scheduled(cron = "0 45 3 * * *", zone = "Asia/Jakarta")
    @Transactional
    public void cleanup() {
        int deleted = tokenRepository.deleteStale(clock.instant());
        log.info("Telegram cleanup: {} link token dihapus", deleted);
    }
}
