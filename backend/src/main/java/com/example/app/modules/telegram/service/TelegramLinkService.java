// filepath: /backend/src/main/java/com/example/app/modules/telegram/service/TelegramLinkService.java
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class TelegramLinkService {

    private static final Logger log = LoggerFactory.getLogger(TelegramLinkService.class);

    static final String MSG_LINKED = "Akun berhasil terhubung ✅\nSekarang kamu bisa mencatat langsung dari chat ini.\nCoba kirim: kopi susu 25rb\nKetik /bantuan untuk contoh lainnya.";
    static final String MSG_INVALID_TOKEN = "Link sudah kedaluwarsa atau tidak valid. Masuk ke web SakuTahu, lalu buat link baru dari menu \"Hubungkan Telegram\".";
    static final String MSG_ALREADY_USED = "Akun Telegram ini sudah terhubung ke akun SakuTahu lain. Putuskan dulu dari akun tersebut.";
    static final String MSG_HOW_TO_LINK = "Halo! 👋 Akun Telegram ini belum terhubung ke SakuTahu.\n1. Masuk ke web SakuTahu dengan email & password kamu\n2. Buka menu \"Hubungkan Telegram\"\n3. Klik \"Buka Telegram\" dan tekan Start di sini";
    static final String MSG_PRIVATE_ONLY = "Silakan hubungkan akun lewat chat pribadi dengan bot ini.";
    static final String MSG_LINKED_HELP = "Akun kamu sudah terhubung ✅\nKirim pesan seperti \"kopi 25rb\" untuk mencatat, atau ketik /bantuan.";

    private final TelegramLinkTokenRepository tokenRepository;
    private final TelegramAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AppProperties props;
    private final Clock clock;

    public TelegramLinkService(TelegramLinkTokenRepository tokenRepository,
                               TelegramAccountRepository accountRepository,
                               UserRepository userRepository,
                               AppProperties props,
                               Clock clock) {
        this.tokenRepository = tokenRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public TelegramLinkResponse createLinkToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isEmailVerified()) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        Instant now = clock.instant();
        tokenRepository.invalidateActiveForUser(userId, now);

        String raw = TokenUtils.randomToken();
        Instant expiresAt = now.plus(props.telegram().linkTokenTtl());
        tokenRepository.save(new TelegramLinkToken(userId, TokenUtils.sha256Hex(raw), expiresAt));

        String botUsername = props.telegram().botUsername();
        String deepLink = "https://t.me/" + botUsername + "?start=" + raw;
        return new TelegramLinkResponse(deepLink, botUsername, expiresAt);
    }

    /** Memproses update dari webhook. Mengembalikan balasan untuk dikirim ke user, jika ada. */
    @Transactional
    public Optional<TelegramWebhookReply> handleUpdate(TelegramUpdate update) {
        TelegramUpdate.Message msg = update.message();
        if (msg == null || msg.text() == null || msg.from() == null || msg.chat() == null) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(msg.from().isBot())) {
            return Optional.empty();
        }

        long chatId = msg.chat().id();
        long telegramUserId = msg.from().id();
        String text = msg.text().trim();

        if (!text.startsWith("/start")) {
            boolean linked = accountRepository.findByTelegramUserId(telegramUserId).isPresent();
            return reply(chatId, linked ? MSG_LINKED_HELP : MSG_HOW_TO_LINK);
        }

        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            boolean linked = accountRepository.findByTelegramUserId(telegramUserId).isPresent();
            return reply(chatId, linked ? MSG_LINKED_HELP : MSG_HOW_TO_LINK);
        }
        if (!"private".equals(msg.chat().type())) {
            return reply(chatId, MSG_PRIVATE_ONLY);
        }

        Instant now = clock.instant();
        Optional<TelegramLinkToken> tokenOpt = tokenRepository.findByTokenHash(TokenUtils.sha256Hex(parts[1].trim()));
        if (tokenOpt.isEmpty() || !tokenOpt.get().isUsable(now)) {
            return reply(chatId, MSG_INVALID_TOKEN);
        }
        TelegramLinkToken token = tokenOpt.get();

        Optional<TelegramAccount> byTelegram = accountRepository.findByTelegramUserId(telegramUserId);
        if (byTelegram.isPresent() && !byTelegram.get().getUserId().equals(token.getUserId())) {
            return reply(chatId, MSG_ALREADY_USED);
        }

        TelegramAccount account = accountRepository.findByUserId(token.getUserId())
                .orElseGet(() -> new TelegramAccount(token.getUserId()));
        account.link(telegramUserId, chatId, msg.from().username(), now);
        accountRepository.save(account);
        token.markUsed(now);
        tokenRepository.save(token);

        log.info("Telegram user {} terhubung ke user {}", telegramUserId, token.getUserId());
        return reply(chatId, MSG_LINKED);
    }

    private static Optional<TelegramWebhookReply> reply(long chatId, String text) {
        return Optional.of(TelegramWebhookReply.sendMessage(chatId, text));
    }
}
