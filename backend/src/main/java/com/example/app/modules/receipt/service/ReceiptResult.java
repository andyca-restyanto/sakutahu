// filepath: /backend/src/main/java/com/example/app/modules/receipt/service/ReceiptResult.java
package com.example.app.modules.receipt.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Hasil membaca struk.
 * @param isReceipt  false jika foto bukan struk belanja
 * @param total      jumlah akhir yang dibayar (null jika tidak terbaca)
 * @param confident  true hanya jika total terbaca jelas dan yakin
 */
public record ReceiptResult(
        boolean isReceipt,
        BigDecimal total,
        String currency,
        String merchant,
        LocalDate receiptDate,
        boolean confident
) {
    /** Total bisa langsung dicatat tanpa konfirmasi user. */
    public boolean usable() {
        return isReceipt && confident && total != null && total.signum() > 0
                && (currency == null || "IDR".equalsIgnoreCase(currency));
    }
}
