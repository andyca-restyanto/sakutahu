// filepath: /backend/src/main/java/com/example/app/modules/report/service/JournalFilter.java
package com.example.app.modules.report.service;

import com.example.app.modules.category.entity.TransactionType;

import java.time.LocalDate;
import java.util.UUID;

/** Filter laporan yang sudah divalidasi. userId SELALU dari token, tidak pernah dari request. */
public record JournalFilter(UUID userId, LocalDate from, LocalDate to, TransactionType type, Long categoryId) {

    public boolean filtered() {
        return type != null || categoryId != null;
    }
}
