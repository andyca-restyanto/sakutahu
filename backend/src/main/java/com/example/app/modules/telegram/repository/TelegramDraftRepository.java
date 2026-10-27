// filepath: /backend/src/main/java/com/example/app/modules/telegram/repository/TelegramDraftRepository.java
package com.example.app.modules.telegram.repository;

import com.example.app.modules.telegram.entity.TelegramDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TelegramDraftRepository extends JpaRepository<TelegramDraft, Long> {

    Optional<TelegramDraft> findByIdAndUserId(Long id, UUID userId);

    Optional<TelegramDraft> findByChatIdAndSourceMessageId(Long chatId, Long sourceMessageId);

    @Modifying
    @Query("delete from TelegramDraft d where d.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
