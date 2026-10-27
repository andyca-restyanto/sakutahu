// filepath: /backend/src/main/java/com/example/app/modules/telegram/service/TelegramAccountService.java
package com.example.app.modules.telegram.service;

import com.example.app.modules.telegram.dto.TelegramStatusResponse;
import com.example.app.modules.telegram.repository.TelegramAccountRepository;
import com.example.app.shared.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** API publik modul telegram untuk modul lain (mis. auth). */
@Service
public class TelegramAccountService {

    private final TelegramAccountRepository accountRepository;
    private final String botUsername;

    public TelegramAccountService(TelegramAccountRepository accountRepository, AppProperties props) {
        this.accountRepository = accountRepository;
        this.botUsername = props.telegram().botUsername();
    }

    @Transactional(readOnly = true)
    public boolean isLinked(UUID userId) {
        return accountRepository.existsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public TelegramStatusResponse status(UUID userId) {
        return accountRepository.findByUserId(userId)
                .map(a -> new TelegramStatusResponse(true, botUsername, a.getUsername(), a.getLinkedAt()))
                .orElseGet(() -> TelegramStatusResponse.notLinked(botUsername));
    }

    @Transactional
    public void unlink(UUID userId) {
        accountRepository.deleteByUserId(userId);
    }
}
