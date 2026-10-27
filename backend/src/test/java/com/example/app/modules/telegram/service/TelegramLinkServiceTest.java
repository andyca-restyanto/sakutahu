// filepath: /backend/src/test/java/com/example/app/modules/telegram/service/TelegramLinkServiceTest.java
package com.example.app.modules.telegram.service;

import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.telegram.dto.TelegramLinkResponse;
import com.example.app.modules.telegram.dto.TelegramUpdate;
import com.example.app.modules.telegram.dto.TelegramWebhookReply;
import com.example.app.modules.telegram.entity.TelegramAccount;
import com.example.app.modules.telegram.entity.TelegramLinkToken;
import com.example.app.modules.telegram.repository.TelegramAccountRepository;
import com.example.app.modules.telegram.repository.TelegramLinkTokenRepository;
import com.example.app.shared.config.AppProperties;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramLinkServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T08:00:00Z");
    private static final long TG_USER_ID = 123456789L;
    private static final long CHAT_ID = 123456789L;

    @Mock TelegramLinkTokenRepository tokenRepository;
    @Mock TelegramAccountRepository accountRepository;
    @Mock UserRepository userRepository;

    private TelegramLinkService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(null, null, List.of(), null, null, null, null, null,
                new AppProperties.Telegram("bot-token", "SakuTahuBot", "secret", Duration.ofMinutes(10)));
        service = new TelegramLinkService(tokenRepository, accountRepository, userRepository, props,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ---- createLinkToken ----

    @Test
    void createLinkTokenReturnsDeepLinkAndBotUsername() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(verifiedUser()));

        TelegramLinkResponse response = service.createLinkToken(userId);

        assertThat(response.botUsername()).isEqualTo("SakuTahuBot");
        assertThat(response.deepLink()).startsWith("https://t.me/SakuTahuBot?start=");
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        verify(tokenRepository).invalidateActiveForUser(userId, NOW);
        verify(tokenRepository).save(any(TelegramLinkToken.class));
    }

    @Test
    void createLinkTokenRejectsUnverifiedUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(unverifiedUser()));

        assertThatThrownBy(() -> service.createLinkToken(userId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
        verify(tokenRepository, never()).save(any());
    }

    // ---- handleUpdate (/start <token>) ----

    @Test
    void validTokenLinksAccount() {
        UUID userId = UUID.randomUUID();
        String raw = "valid-token";
        TelegramLinkToken token = new TelegramLinkToken(userId, TokenUtils.sha256Hex(raw), NOW.plusSeconds(300));
        when(tokenRepository.findByTokenHash(TokenUtils.sha256Hex(raw))).thenReturn(Optional.of(token));

        Optional<TelegramWebhookReply> reply = service.handleUpdate(start(raw));

        assertThat(reply).map(TelegramWebhookReply::text).contains(TelegramLinkService.MSG_LINKED);
        ArgumentCaptor<TelegramAccount> saved = ArgumentCaptor.forClass(TelegramAccount.class);
        verify(accountRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getTelegramUserId()).isEqualTo(TG_USER_ID);
        assertThat(saved.getValue().getChatId()).isEqualTo(CHAT_ID);
        assertThat(saved.getValue().getUsername()).isEqualTo("budisan");
        assertThat(token.isUsable(NOW)).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        String raw = "expired-token";
        TelegramLinkToken token = new TelegramLinkToken(UUID.randomUUID(), TokenUtils.sha256Hex(raw), NOW.minusSeconds(1));
        when(tokenRepository.findByTokenHash(TokenUtils.sha256Hex(raw))).thenReturn(Optional.of(token));

        assertThat(service.handleUpdate(start(raw)))
                .map(TelegramWebhookReply::text).contains(TelegramLinkService.MSG_INVALID_TOKEN);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void unknownTokenIsRejected() {
        assertThat(service.handleUpdate(start("tidak-ada")))
                .map(TelegramWebhookReply::text).contains(TelegramLinkService.MSG_INVALID_TOKEN);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void telegramAccountAlreadyLinkedToAnotherUserIsRejected() {
        String raw = "valid-token";
        TelegramLinkToken token = new TelegramLinkToken(UUID.randomUUID(), TokenUtils.sha256Hex(raw), NOW.plusSeconds(300));
        when(tokenRepository.findByTokenHash(TokenUtils.sha256Hex(raw))).thenReturn(Optional.of(token));
        TelegramAccount other = new TelegramAccount(UUID.randomUUID());
        other.link(TG_USER_ID, CHAT_ID, "budisan", NOW.minusSeconds(3600));
        when(accountRepository.findByTelegramUserId(TG_USER_ID)).thenReturn(Optional.of(other));

        assertThat(service.handleUpdate(start(raw)))
                .map(TelegramWebhookReply::text).contains(TelegramLinkService.MSG_ALREADY_USED);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void startWithoutTokenAsksUserToLoginOnWeb() {
        assertThat(service.handleUpdate(message("/start", "private")))
                .map(TelegramWebhookReply::text).contains(TelegramLinkService.MSG_HOW_TO_LINK);
    }

    @Test
    void groupChatIsRejected() {
        assertThat(service.handleUpdate(message("/start abc", "group")))
                .map(TelegramWebhookReply::text).contains(TelegramLinkService.MSG_PRIVATE_ONLY);
    }

    // ---- helpers ----

    private static TelegramUpdate start(String rawToken) {
        return message("/start " + rawToken, "private");
    }

    private static TelegramUpdate message(String text, String chatType) {
        return new TelegramUpdate(1L, new TelegramUpdate.Message(
                10L,
                new TelegramUpdate.From(TG_USER_ID, false, "Budi", "budisan"),
                new TelegramUpdate.Chat(CHAT_ID, chatType),
                text), null);
    }

    private static User verifiedUser() {
        User user = unverifiedUser();
        user.markEmailVerified(NOW);
        return user;
    }

    private static User unverifiedUser() {
        return new User("budi@mail.com", "hash", "Budi Santoso", "IDR", "Asia/Jakarta");
    }
}
