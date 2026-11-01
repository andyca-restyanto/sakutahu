// filepath: /backend/src/main/java/com/example/app/modules/report/dto/JournalEntryResponse.java
package com.example.app.modules.report.dto;

import com.example.app.modules.category.dto.CategorySummaryResponse;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.transaction.entity.TransactionSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Satu baris buku kas. income = kolom Masuk, expense = kolom Keluar (termasuk tabungan), salah satunya 0.
 * balance = saldo setelah baris ini; null jika laporan difilter.
 */
public record JournalEntryResponse(
        UUID id,
        LocalDate occurredOn,
        String description,
        CategorySummaryResponse category,
        TransactionType type,
        TransactionSource source,
        UUID receiptScanId,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal balance,
        Instant createdAt
) {}
