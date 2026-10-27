// filepath: /backend/src/main/java/com/example/app/modules/telegram/service/TelegramBotClient.java
package com.example.app.modules.telegram.service;

import com.example.app.modules.telegram.dto.TelegramWebhookReply.InlineKeyboard;
import com.example.app.shared.config.AppProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Panggilan keluar ke Telegram Bot API. Balasan cepat tetap lewat response webhook; client ini untuk
 * aksi asinkron (hasil baca struk) dan aksi yang tidak bisa digabung di webhook (answerCallbackQuery, unduh file).
 */
@Component
public class TelegramBotClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);

    private final RestClient restClient;
    private final String botToken;

    public TelegramBotClient(AppProperties props,
                             @Value("${app.telegram.api-base-url:https://api.telegram.org}") String apiBaseUrl) {
        this.botToken = props.telegram().botToken();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(20_000);
        this.restClient = RestClient.builder().baseUrl(apiBaseUrl).requestFactory(factory).build();
    }

    /** Menghilangkan ikon loading di tombol; text opsional tampil sebagai toast kecil di Telegram. */
    public void answerCallbackQuery(String callbackQueryId, String text) {
        if (!configured()) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("callback_query_id", callbackQueryId);
        if (text != null) {
            body.put("text", text);
        }
        post("answerCallbackQuery", body);
    }

    /** @return message_id pesan yang terkirim, jika berhasil */
    public Optional<Long> sendMessage(long chatId, String text, InlineKeyboard keyboard) {
        if (!configured()) {
            return Optional.empty();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        if (keyboard != null) {
            body.put("reply_markup", keyboard);
        }
        return post("sendMessage", body).map(r -> r.result() == null ? null : r.result().messageId());
    }

    public void editMessageText(long chatId, long messageId, String text, InlineKeyboard keyboard) {
        if (!configured()) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("message_id", messageId);
        body.put("text", text);
        if (keyboard != null) {
            body.put("reply_markup", keyboard);
        }
        post("editMessageText", body);
    }

    /**
     * Unduh file dari server Telegram (getFile → file_path → download).
     * @throws FileTooLargeException jika melebihi maxBytes
     * @throws IOException jika gagal mengunduh
     */
    public byte[] downloadFile(String fileId, long maxBytes) throws IOException {
        if (!configured()) {
            throw new IOException("TELEGRAM_BOT_TOKEN belum diisi");
        }
        FileResponse file;
        try {
            file = restClient.get()
                    .uri("/bot{token}/getFile?file_id={fileId}", botToken, fileId)
                    .retrieve()
                    .body(FileResponse.class);
        } catch (RestClientException e) {
            throw new IOException("getFile gagal: " + e.getMessage(), e);
        }
        if (file == null || !file.ok() || file.result() == null || file.result().filePath() == null) {
            throw new IOException("getFile tidak mengembalikan file_path");
        }
        if (file.result().fileSize() != null && file.result().fileSize() > maxBytes) {
            throw new FileTooLargeException();
        }
        try {
            byte[] bytes = restClient.get()
                    .uri("/file/bot{token}/{path}", botToken, file.result().filePath())
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new IOException("Unduh file gagal: HTTP " + response.getStatusCode().value());
                        }
                        try (InputStream in = response.getBody()) {
                            byte[] data = in.readNBytes((int) Math.min(maxBytes + 1, Integer.MAX_VALUE - 8));
                            if (data.length > maxBytes) {
                                throw new FileTooLargeException();
                            }
                            return data;
                        }
                    });
            if (bytes == null) {
                throw new IOException("File kosong");
            }
            return bytes;
        } catch (RestClientException e) {
            if (e.getCause() instanceof IOException io) {
                throw io;
            }
            throw new IOException("Unduh file gagal: " + e.getMessage(), e);
        }
    }

    private boolean configured() {
        if (botToken == null || botToken.isBlank()) {
            log.warn("TELEGRAM_BOT_TOKEN kosong, panggilan Bot API dilewati");
            return false;
        }
        return true;
    }

    private Optional<SendResponse> post(String method, Map<String, Object> body) {
        try {
            return Optional.ofNullable(restClient.post()
                    .uri("/bot{token}/{method}", botToken, method)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(SendResponse.class));
        } catch (RestClientException e) {
            log.warn("Telegram {} gagal: {}", method, e.getMessage());
            return Optional.empty();
        }
    }

    /** File melebihi batas ukuran yang diizinkan. */
    public static class FileTooLargeException extends IOException {
        public FileTooLargeException() {
            super("File terlalu besar");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendResponse(boolean ok, SentMessage result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SentMessage(@JsonProperty("message_id") Long messageId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FileResponse(boolean ok, FileInfo result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FileInfo(@JsonProperty("file_path") String filePath, @JsonProperty("file_size") Long fileSize) {}
}
