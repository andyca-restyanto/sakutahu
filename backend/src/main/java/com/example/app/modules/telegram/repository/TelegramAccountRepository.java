// filepath: /backend/src/main/java/com/example/app/modules/telegram/repository/TelegramAccountRepository.java
package com.example.app.modules.telegram.repository;

import com.example.app.modules.telegram.entity.TelegramAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TelegramAccountRepository extends JpaRepository<TelegramAccount, UUID> {

    Optional<TelegramAccount> findByUserId(UUID userId);

    Optional<TelegramAccount> findByTelegramUserId(Long telegramUserId);

    boolean existsByUserId(UUID userId);

    long deleteByUserId(UUID userId);
}
