// filepath: /backend/src/main/java/com/example/app/modules/telegram/service/TelegramUpdateRouter.java
package com.example.app.modules.telegram.service;

import com.example.app.modules.receipt.service.ReceiptScanService;
import com.example.app.modules.telegram.dto.TelegramUpdate;
import com.example.app.modules.telegram.dto.TelegramWebhookReply;
import com.example.app.modules.telegram.entity.TelegramAccount;
import com.example.app.modules.telegram.repository.TelegramAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Pintu masuk semua update webhook.
 * - Tombol (callback_query) → pencatatan, hanya untuk akun tertaut.
 * - "/start ..." atau pengirim yang belum tertaut → alur penautan akun.
 * - Foto/gambar dari akun tertaut → pencatatan dari struk (modul receipt).
 * - Pesan lain dari akun tertaut di chat pribadi → pencatatan transaksi.
 * Identitas user SELALU dari telegram_user_id yang tertaut, tidak pernah dari isi pesan.
 */
@Service
public class TelegramUpdateRouter {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateRouter.class);
    static final String ERROR_TEXT = "Maaf, terjadi kesalahan saat memproses pesanmu. Coba lagi sebentar lagi ya.";

    private final TelegramLinkService linkService;
    private final TelegramRecordingService recordingService;
    private final TelegramAccountRepository accountRepository;
    private final TelegramBotClient botClient;
    private final ReceiptScanService receiptScanService;

    public TelegramUpdateRouter(TelegramLinkService linkService,
                                TelegramRecordingService recordingService,
                                TelegramAccountRepository accountRepository,
                                TelegramBotClient botClient,
                                ReceiptScanService receiptScanService) {
        this.linkService = linkService;
        this.recordingService = recordingService;
        this.accountRepository = accountRepository;
        this.botClient = botClient;
        this.receiptScanService = receiptScanService;
    }

    public Optional<TelegramWebhookReply> route(TelegramUpdate update) {
        if (update.callbackQuery() != null) {
            return handleCallback(update.callbackQuery());
        }

        TelegramUpdate.Message msg = update.message();
        if (msg == null || msg.from() == null || msg.chat() == null || msg.messageId() == null
                || Boolean.TRUE.equals(msg.from().isBot())) {
            return Optional.empty();
        }

        String text = msg.text() == null ? "" : msg.text().trim();
        boolean isStart = text.toLowerCase(java.util.Locale.ROOT).startsWith("/start");
        boolean isPrivate = "private".equals(msg.chat().type());
        Optional<TelegramAccount> account = accountRepository.findByTelegramUserId(msg.from().id());

        if (isStart || account.isEmpty() || !isPrivate) {
            return linkService.handleUpdate(update);
        }
        try {
            var userId = account.get().getUserId();
            // Foto / gambar → pencatatan dari struk
            if (msg.hasImage()) {
                return receiptScanService.handleImage(userId, msg);
            }
            // Nominal yang ditunggu untuk struk yang totalnya tidak terbaca
            Optional<TelegramWebhookReply> receiptAmount = receiptScanService.handleAmountReply(userId, msg);
            if (receiptAmount.isPresent()) {
                return receiptAmount;
            }
            return Optional.of(recordingService.handleMessage(userId, msg));
        } catch (RuntimeException e) {
            // Tetap balas 200 agar Telegram tidak mengirim ulang terus-menerus; user diberi tahu
            log.error("Gagal memproses pesan Telegram dari user {}", account.get().getUserId(), e);
            return Optional.of(TelegramWebhookReply.sendMessage(msg.chat().id(), ERROR_TEXT));
        }
    }

    private Optional<TelegramWebhookReply> handleCallback(TelegramUpdate.CallbackQuery cq) {
        if (cq.from() == null || cq.message() == null || cq.message().chat() == null || cq.message().messageId() == null) {
            return Optional.empty();
        }
        Optional<TelegramAccount> account = accountRepository.findByTelegramUserId(cq.from().id());
        if (account.isEmpty()) {
            botClient.answerCallbackQuery(cq.id(), "Akun Telegram ini belum terhubung ke SakuTahu");
            return Optional.empty();
        }
        botClient.answerCallbackQuery(cq.id(), null);
        try {
            String data = cq.data() == null ? "" : cq.data();
            if (data.startsWith("r:")) {
                return Optional.of(receiptScanService.handleSkip(account.get().getUserId(), cq.message().chat().id(),
                        cq.message().messageId(), java.util.UUID.fromString(data.substring(2))));
            }
            return Optional.of(recordingService.handleCallback(account.get().getUserId(), cq));
        } catch (RuntimeException e) {
            log.error("Gagal memproses tombol Telegram dari user {}", account.get().getUserId(), e);
            return Optional.of(TelegramWebhookReply.sendMessage(cq.message().chat().id(), ERROR_TEXT));
        }
    }
}
