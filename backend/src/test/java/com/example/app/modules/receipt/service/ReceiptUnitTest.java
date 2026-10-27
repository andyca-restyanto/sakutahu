// filepath: /backend/src/test/java/com/example/app/modules/receipt/service/ReceiptUnitTest.java
package com.example.app.modules.receipt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceiptUnitTest {

    private final ClaudeVisionReceiptReader reader = new ClaudeVisionReceiptReader(
            new ReceiptProperties("claude", "k", "claude-haiku-5-5", "http://localhost:1", 7, 5, null), new ObjectMapper());

    @Test
    void parsesModelJsonEvenWithCodeFences() throws Exception {
        ReceiptResult r = reader.parse("""
                ```json
                {"is_receipt": true, "total": 125400, "currency": "IDR", "merchant": "Indomaret", "date": "2026-10-12", "confidence": "high"}
                ```""");
        assertThat(r.usable()).isTrue();
        assertThat(r.total()).isEqualByComparingTo("125400");
        assertThat(r.merchant()).isEqualTo("Indomaret");
        assertThat(r.receiptDate()).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void lowConfidenceOrForeignCurrencyIsNotUsable() throws Exception {
        assertThat(reader.parse("{\"is_receipt\":true,\"total\":5000,\"currency\":\"IDR\",\"confidence\":\"low\"}").usable()).isFalse();
        assertThat(reader.parse("{\"is_receipt\":true,\"total\":12.5,\"currency\":\"USD\",\"confidence\":\"high\"}").usable()).isFalse();
        assertThat(reader.parse("{\"is_receipt\":false,\"total\":null,\"confidence\":\"low\"}").isReceipt()).isFalse();
    }

    @Test
    void textualTotalsAreParsed() {
        assertThat(ClaudeVisionReceiptReader.parseLooseNumber("125.400")).isEqualByComparingTo("125400");
        assertThat(ClaudeVisionReceiptReader.parseLooseNumber("Rp 1.250.000,50")).isEqualByComparingTo("1250000.50");
        assertThat(ClaudeVisionReceiptReader.parseLooseNumber("25000")).isEqualByComparingTo("25000");
    }

    @Test
    void rejectsNonJson() {
        assertThatThrownBy(() -> reader.parse("Maaf saya tidak bisa membaca gambar ini"))
                .isInstanceOf(ReceiptReader.ReceiptReadException.class);
    }

    @Test
    void apiErrorsAreClassifiedWithReadableMessage() {
        var credit = reader.toReadException(400, """
                {"type":"error","error":{"type":"invalid_request_error","message":"Your credit balance is too low to access the Anthropic API."},"request_id":"req_1"}""", null);
        assertThat(credit.kind()).isEqualTo(ReceiptReader.FailureKind.SERVICE_CONFIG);
        assertThat(credit.getMessage()).contains("400 invalid_request_error").contains("credit balance").contains("req_1");

        assertThat(reader.toReadException(401, "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key\"}}", null).kind())
                .isEqualTo(ReceiptReader.FailureKind.SERVICE_CONFIG);
        assertThat(reader.toReadException(529, "{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\",\"message\":\"Overloaded\"}}", null).kind())
                .isEqualTo(ReceiptReader.FailureKind.SERVICE_BUSY);
        assertThat(reader.toReadException(400, "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"message\":\"Could not process image\"}}", null).kind())
                .isEqualTo(ReceiptReader.FailureKind.BAD_IMAGE);
        assertThat(reader.toReadException(502, "<html>Bad Gateway</html>", null).kind())
                .isEqualTo(ReceiptReader.FailureKind.SERVICE_BUSY);
    }

    @Test
    void detectsImageTypeFromMagicBytes() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        byte[] pdf = {'%', 'P', 'D', 'F', '-', '1', '.', '4', 0, 0, 0, 0};
        assertThat(ReceiptScanService.detectMediaType(jpeg)).isEqualTo("image/jpeg");
        assertThat(ReceiptScanService.detectMediaType(png)).isEqualTo("image/png");
        assertThat(ReceiptScanService.detectMediaType(webp)).isEqualTo("image/webp");
        assertThat(ReceiptScanService.detectMediaType(pdf)).isNull();
    }
}
