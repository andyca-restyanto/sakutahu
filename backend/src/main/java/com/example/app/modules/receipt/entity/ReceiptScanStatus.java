// filepath: /backend/src/main/java/com/example/app/modules/receipt/entity/ReceiptScanStatus.java
package com.example.app.modules.receipt.entity;

public enum ReceiptScanStatus {
    /** Diterima, menunggu diproses. */
    PENDING,
    /** Sedang diunduh & dibaca. */
    PROCESSING,
    /** Tercatat sebagai transaksi. */
    DONE,
    /** Total tidak terbaca yakin; menunggu user mengetik nominal (15 menit). */
    NEEDS_AMOUNT,
    /** Gagal / bukan struk / kedaluwarsa. Tidak memotong kuota. */
    FAILED
}
