// filepath: /backend/src/main/java/com/example/app/modules/report/dto/JournalSummaryResponse.java
package com.example.app.modules.report.dto;

import java.math.BigDecimal;

/**
 * Ringkasan periode. openingBalance & closingBalance = null jika laporan difilter (jenis/kategori),
 * karena saldo dari sebagian transaksi tidak bermakna. totalExpense TIDAK termasuk tabungan (totalSaving).
 */
public record JournalSummaryResponse(
        BigDecimal openingBalance,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal totalSaving,
        BigDecimal closingBalance,
        long entryCount
) {}
