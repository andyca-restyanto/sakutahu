// filepath: /backend/src/main/java/com/example/app/modules/telegram/repository/TelegramLinkTokenRepository.java
package com.example.app.modules.telegram.repository;

import com.example.app.modules.telegram.entity.TelegramLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TelegramLinkTokenRepository extends JpaRepository<TelegramLinkToken, UUID> {

    Optional<TelegramLinkToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update TelegramLinkToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    int invalidateActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from TelegramLinkToken t where t.expiresAt < :now or t.usedAt is not null")
    int deleteStale(@Param("now") Instant now);
}
