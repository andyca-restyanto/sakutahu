// filepath: /backend/src/main/java/com/example/app/modules/transaction/dto/TransactionResponse.java
package com.example.app.modules.transaction.dto;

import com.example.app.modules.category.dto.CategorySummaryResponse;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.transaction.entity.TransactionSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        CategorySummaryResponse category,
        BigDecimal amount,
        LocalDate occurredOn,
        String note,
        TransactionSource source,
        UUID receiptScanId,
        Instant createdAt
) {}
