// filepath: /backend/src/main/java/com/example/app/modules/receipt/service/ClaudeVisionReceiptReader.java
package com.example.app.modules.receipt.service;

import com.example.app.modules.receipt.service.ReceiptReader.FailureKind;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Membaca struk dengan Claude (Messages API + image base64). Docs:
 * https://platform.claude.com/docs/en/build-with-claude/vision
 * Model default claude-haiku-5-5 (tercepat & termurah); bisa diganti lewat RECEIPT_AI_MODEL.
 */
@Component
public class ClaudeVisionReceiptReader implements ReceiptReader {

    private static final Logger log = LoggerFactory.getLogger(ClaudeVisionReceiptReader.class);
    private static final String API_VERSION = "2023-06-01";

    static final String PROMPT = """
            Kamu membaca foto struk belanja dari Indonesia. Balas HANYA dengan satu objek JSON, tanpa teks lain:
            {"is_receipt": boolean, "total": number|null, "currency": string|null, "merchant": string|null,
             "date": "YYYY-MM-DD"|null, "confidence": "high"|"low"}

            Aturan:
            - "total" = jumlah AKHIR yang dibayar pembeli (label seperti TOTAL, GRAND TOTAL, TOTAL BAYAR,
              TOTAL BELANJA, JUMLAH). BUKAN subtotal sebelum diskon/pajak, BUKAN uang TUNAI/CASH yang diserahkan,
              BUKAN KEMBALIAN/CHANGE, BUKAN poin.
            - Format angka Indonesia: titik = pemisah ribuan, koma = desimal. "125.400" = 125400. Tulis "total" sebagai angka biasa.
            - "currency": kode ISO, biasanya "IDR".
            - "merchant": nama toko singkat (maks 60 karakter), mis. "Indomaret Kebayoran".
            - "date": tanggal transaksi di struk jika ada.
            - "confidence": "high" hanya jika angka total terbaca jelas dan kamu yakin itu jumlah akhir;
              selain itu "low" (foto buram, terpotong, beberapa kandidat total).
            - Jika foto bukan struk/nota/bukti pembayaran: is_receipt=false dan total=null.
            """;

    private final ReceiptProperties props;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ClaudeVisionReceiptReader(ReceiptProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout((props.timeoutSeconds() == null ? 45 : props.timeoutSeconds()) * 1000);
        this.restClient = RestClient.builder()
                .baseUrl(props.apiBaseUrl() == null ? "https://api.anthropic.com" : props.apiBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public ReceiptResult read(byte[] image, String mediaType) throws ReceiptReadException {
        if (!props.configured()) {
            throw new ReceiptReadException("ANTHROPIC_API_KEY belum diisi");
        }
        Map<String, Object> body = Map.of(
                "model", props.model() == null ? "claude-haiku-5-5" : props.model(),
                "max_tokens", 300,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "image", "source", Map.of(
                                        "type", "base64",
                                        "media_type", mediaType,
                                        "data", Base64.getEncoder().encodeToString(image))),
                                Map.of("type", "text", "text", PROMPT)))));

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", props.apiKey())
                    .header("anthropic-version", API_VERSION)
                    .headers(h -> {
                        // API key multi-workspace wajib menyebut workspace tujuan
                        if (props.workspaceId() != null && !props.workspaceId().isBlank()) {
                            h.set("anthropic-workspace-id", props.workspaceId().trim());
                        }
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw toReadException(e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new ReceiptReadException(FailureKind.SERVICE_BUSY,
                    "Claude API tidak bisa dihubungi: " + e.getMessage(), e);
        }

        String text = extractText(response);
        return parse(text);
    }

    /**
     * Ubah error API jadi exception yang jelas. Body error Anthropic:
     * {"type":"error","error":{"type":"invalid_request_error","message":"..."},"request_id":"req_..."}
     * Body request (berisi gambar & API key di header) tidak pernah di-log.
     */
    ReceiptReadException toReadException(int status, String responseBody, Throwable cause) {
        String type = "unknown";
        String message = responseBody == null ? "" : abbreviate(responseBody);
        String requestId = null;
        try {
            JsonNode err = objectMapper.readTree(responseBody);
            type = err.path("error").path("type").asText(type);
            message = err.path("error").path("message").asText(message);
            requestId = textOrNull(err.path("request_id"));
        } catch (Exception ignored) {
            // body bukan JSON (mis. dari proxy); pakai teks mentah
        }

        FailureKind kind = classify(status, type, message);
        String detail = "Claude API error " + status + " " + type + ": " + message
                + (requestId != null ? " (request_id " + requestId + ")" : "");
        if (kind == FailureKind.SERVICE_CONFIG) {
            log.error("{} — periksa ANTHROPIC_API_KEY, ANTHROPIC_WORKSPACE_ID, saldo kredit, dan RECEIPT_AI_MODEL", detail);
        } else {
            log.warn(detail);
        }
        return new ReceiptReadException(kind, detail, cause);
    }

    static FailureKind classify(int status, String type, String message) {
        String m = message == null ? "" : message.toLowerCase();
        if (status == 401 || status == 402 || status == 403 || status == 404
                || m.contains("credit balance") || m.contains("billing") || m.contains("model")
                || m.contains("workspace") || m.contains("api key")) {
            return FailureKind.SERVICE_CONFIG;
        }
        if (status == 408 || status == 429 || status == 504 || status == 529 || status >= 500) {
            return FailureKind.SERVICE_BUSY;
        }
        if (status == 413 || m.contains("image")) {
            return FailureKind.BAD_IMAGE;
        }
        return FailureKind.SERVICE_CONFIG;
    }

    private static String extractText(JsonNode response) throws ReceiptReadException {
        if (response == null || !response.has("content")) {
            throw new ReceiptReadException("Respons Claude kosong");
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : response.get("content")) {
            if ("text".equals(block.path("type").asText())) {
                sb.append(block.path("text").asText());
            }
        }
        if (sb.isEmpty()) {
            throw new ReceiptReadException("Respons Claude tidak berisi teks");
        }
        return sb.toString();
    }

    /** Ambil objek JSON dari teks model (toleran terhadap ```json fences atau teks tambahan). */
    ReceiptResult parse(String text) throws ReceiptReadException {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new ReceiptReadException("Respons bukan JSON: " + abbreviate(text));
        }
        JsonNode json;
        try {
            json = objectMapper.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            throw new ReceiptReadException("JSON tidak valid: " + abbreviate(text), e);
        }

        boolean isReceipt = json.path("is_receipt").asBoolean(false);
        BigDecimal total = null;
        JsonNode totalNode = json.path("total");
        if (totalNode.isNumber()) {
            total = totalNode.decimalValue();
        } else if (totalNode.isTextual()) {
            total = parseLooseNumber(totalNode.asText());
        }
        String currency = textOrNull(json.path("currency"));
        String merchant = textOrNull(json.path("merchant"));
        if (merchant != null && merchant.length() > 60) {
            merchant = merchant.substring(0, 60).trim();
        }
        LocalDate date = null;
        String rawDate = textOrNull(json.path("date"));
        if (rawDate != null) {
            try {
                date = LocalDate.parse(rawDate);
            } catch (DateTimeParseException ignored) {
                // tanggal di struk opsional
            }
        }
        boolean confident = "high".equalsIgnoreCase(json.path("confidence").asText(""));
        return new ReceiptResult(isReceipt, total, currency, merchant, date, confident);
    }

    /** "125.400" / "125,400" / "Rp 125.400,00" → 125400(.00). */
    static BigDecimal parseLooseNumber(String raw) {
        String s = raw.replaceAll("(?i)rp", "").replaceAll("\\s", "");
        if (s.matches("\\d{1,3}([.,]\\d{3})+([.,]\\d{1,2})?")) {
            String decimals = "";
            if (s.matches(".*[.,]\\d{1,2}$")) {
                int idx = Math.max(s.lastIndexOf('.'), s.lastIndexOf(','));
                decimals = "." + s.substring(idx + 1);
                s = s.substring(0, idx);
            }
            s = s.replaceAll("[.,]", "") + decimals;
        } else {
            s = s.replace(',', '.');
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String v = node.asText().trim();
        return v.isEmpty() ? null : v;
    }

    private static String abbreviate(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
