// filepath: /backend/src/main/java/com/example/app/modules/telegram/dto/TelegramUpdate.java
package com.example.app.modules.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Subset payload Update dari Telegram Bot API yang kita butuhkan (snake_case dari Telegram). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdate(
        @JsonProperty("update_id") Long updateId,
        Message message,
        @JsonProperty("callback_query") CallbackQuery callbackQuery
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("message_id") Long messageId,
            From from,
            Chat chat,
            String text,
            /* Foto dikirim Telegram dalam beberapa ukuran; yang terakhir adalah yang terbesar. */
            List<PhotoSize> photo,
            /* Gambar yang dikirim sebagai "file" (tidak dikompres). */
            Document document,
            String caption
    ) {
        /** Konstruktor ringkas untuk pesan teks (dipakai test & kode lama). */
        public Message(Long messageId, From from, Chat chat, String text) {
            this(messageId, from, chat, text, null, null, null);
        }

        public boolean hasImage() {
            return (photo != null && !photo.isEmpty()) || document != null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PhotoSize(
            @JsonProperty("file_id") String fileId,
            @JsonProperty("file_unique_id") String fileUniqueId,
            Integer width,
            Integer height,
            @JsonProperty("file_size") Long fileSize
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            @JsonProperty("file_id") String fileId,
            @JsonProperty("file_unique_id") String fileUniqueId,
            @JsonProperty("file_name") String fileName,
            @JsonProperty("mime_type") String mimeType,
            @JsonProperty("file_size") Long fileSize
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record From(
            Long id,
            @JsonProperty("is_bot") Boolean isBot,
            @JsonProperty("first_name") String firstName,
            String username
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(Long id, String type) {}

    /** Dikirim saat user menekan tombol inline. `message` adalah pesan bot yang memuat tombol tersebut. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallbackQuery(
            String id,
            From from,
            Message message,
            String data
    ) {}
}
