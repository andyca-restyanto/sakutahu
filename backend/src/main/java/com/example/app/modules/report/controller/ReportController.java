// filepath: /backend/src/main/java/com/example/app/modules/report/controller/ReportController.java
package com.example.app.modules.report.controller;

import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.report.dto.JournalReportResponse;
import com.example.app.modules.report.service.JournalReportService;
import com.example.app.shared.security.UserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;

/** Menu "Laporan": jurnal buku kas + unduh Excel. Data selalu milik user yang login. */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final JournalReportService service;

    public ReportController(JournalReportService service) {
        this.service = service;
    }

    @GetMapping("/journal")
    public ResponseEntity<JournalReportResponse> journal(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.journal(principal.id(), from, to, type, categoryId, page, size));
    }

    @GetMapping("/journal/export")
    public ResponseEntity<StreamingResponseBody> export(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId) {
        // Validasi, rate limit, dan batas jumlah baris dicek sebelum file mulai dikirim
        JournalReportService.PreparedExport export = service.prepareExport(principal.id(), from, to, type, categoryId);
        StreamingResponseBody body = out -> service.writeExport(export, out);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.filename()).build().toString())
                .body(body);
    }
}
