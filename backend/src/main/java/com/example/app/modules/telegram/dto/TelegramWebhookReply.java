// filepath: /backend/src/main/java/com/example/app/modules/telegram/dto/TelegramWebhookReply.java
package com.example.app.modules.telegram.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Balasan langsung di response webhook. Telegram mengeksekusi method ini (sendMessage / editMessageText)
 * tanpa kita perlu memanggil Bot API secara terpisah.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramWebhookReply(
        String method,
        @JsonProperty("chat_id") Long chatId,
        @JsonProperty("message_id") Long messageId,
        String text,
        @JsonProperty("reply_markup") InlineKeyboard replyMarkup
) {
    public record InlineKeyboard(@JsonProperty("inline_keyboard") List<List<Button>> inlineKeyboard) {}

    public record Button(String text, @JsonProperty("callback_data") String callbackData) {}

    public static TelegramWebhookReply sendMessage(long chatId, String text) {
        return new TelegramWebhookReply("sendMessage", chatId, null, text, null);
    }

    public static TelegramWebhookReply sendMessage(long chatId, String text, InlineKeyboard keyboard) {
        return new TelegramWebhookReply("sendMessage", chatId, null, text, keyboard);
    }

    /** Mengganti isi pesan bot yang tombolnya ditekan (keyboard null = tombol dihapus). */
    public static TelegramWebhookReply editMessage(long chatId, long messageId, String text, InlineKeyboard keyboard) {
        return new TelegramWebhookReply("editMessageText", chatId, messageId, text, keyboard);
    }
}
