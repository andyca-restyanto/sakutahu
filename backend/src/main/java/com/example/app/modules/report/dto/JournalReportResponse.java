// filepath: /backend/src/main/java/com/example/app/modules/report/dto/JournalReportResponse.java
package com.example.app.modules.report.dto;

import com.example.app.shared.web.PageResponse;

import java.time.LocalDate;

/** filtered = true jika type/categoryId dipakai (kolom saldo disembunyikan). */
public record JournalReportResponse(
        LocalDate from,
        LocalDate to,
        boolean filtered,
        JournalSummaryResponse summary,
        PageResponse<JournalEntryResponse> entries
) {}
