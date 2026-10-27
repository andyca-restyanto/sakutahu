// filepath: /backend/src/main/java/com/example/app/shared/exception/ErrorCode.java
package com.example.app.shared.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Data tidak valid"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email sudah terdaftar"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email atau password salah"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Email belum diverifikasi"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "Akun dikunci sementara karena terlalu banyak percobaan gagal"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Terlalu banyak permintaan, coba lagi nanti"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Sesi telah berakhir, silakan login kembali"),
    VERIFICATION_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Link verifikasi tidak valid"),
    VERIFICATION_TOKEN_EXPIRED(HttpStatus.GONE, "Link verifikasi sudah kedaluwarsa"),
    CATEGORY_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "Kategori tidak sesuai dengan jenis transaksi"),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Transaksi tidak ditemukan"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Autentikasi diperlukan"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Akses ditolak"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Data tidak ditemukan"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan pada server");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
