// filepath: /backend/src/main/java/com/example/app/modules/receipt/service/ReceiptReader.java
package com.example.app.modules.receipt.service;

/** Pembaca struk. Implementasi bisa diganti (AI vision lain / OCR) tanpa mengubah alur. */
public interface ReceiptReader {

    /**
     * @param image     byte gambar (JPEG/PNG/WebP)
     * @param mediaType mis. "image/jpeg"
     * @throws ReceiptReadException jika layanan gagal dihubungi / respons tidak bisa dipahami
     */
    ReceiptResult read(byte[] image, String mediaType) throws ReceiptReadException;

    /** Jenis kegagalan; menentukan pesan ke user dan error_code di receipt_scans. */
    enum FailureKind {
        /** Masalah di sisi server/admin: API key salah, saldo kredit habis, model tidak ada, dsb. */
        SERVICE_CONFIG,
        /** Gangguan sementara: rate limit, overload, timeout, 5xx. */
        SERVICE_BUSY,
        /** Gambar ditolak API (rusak, terlalu besar, format tidak cocok). */
        BAD_IMAGE,
        /** Respons model tidak bisa dipahami. */
        BAD_RESPONSE
    }

    class ReceiptReadException extends Exception {
        private final FailureKind kind;

        public ReceiptReadException(String message) {
            this(FailureKind.BAD_RESPONSE, message, null);
        }

        public ReceiptReadException(String message, Throwable cause) {
            this(FailureKind.BAD_RESPONSE, message, cause);
        }

        public ReceiptReadException(FailureKind kind, String message, Throwable cause) {
            super(message, cause);
            this.kind = kind;
        }

        public FailureKind kind() {
            return kind;
        }
    }
}
