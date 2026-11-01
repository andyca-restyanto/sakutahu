// filepath: /backend/src/main/java/com/example/app/modules/report/service/JournalReportService.java
package com.example.app.modules.report.service;

import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.category.entity.CategoryKind;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.report.dto.CategoryRecapResponse;
import com.example.app.modules.report.dto.JournalEntryResponse;
import com.example.app.modules.report.dto.JournalReportResponse;
import com.example.app.modules.report.dto.JournalSummaryResponse;
import com.example.app.modules.report.repository.JournalQueryRepository;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.ratelimit.RateLimiter;
import com.example.app.shared.web.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Laporan jurnal (buku kas): ringkasan, baris dengan saldo berjalan, dan export Excel. */
@Service
public class JournalReportService {

    static final int MAX_RANGE_MONTHS = 24;
    static final int MAX_PAGE_SIZE = 200;
    static final int MAX_EXPORT_ROWS = 50_000;

    private final JournalQueryRepository queries;
    private final UserRepository userRepository;
    private final JournalExcelExporter exporter;
    private final RateLimiter rateLimiter;
    private final TransactionTemplate readOnlyTx;
    private final Clock clock;

    public JournalReportService(JournalQueryRepository queries, UserRepository userRepository,
                                JournalExcelExporter exporter, RateLimiter rateLimiter,
                                TransactionTemplate transactionTemplate, Clock clock) {
        this.queries = queries;
        this.userRepository = userRepository;
        this.exporter = exporter;
        this.rateLimiter = rateLimiter;
        this.readOnlyTx = new TransactionTemplate(transactionTemplate.getTransactionManager());
        this.readOnlyTx.setReadOnly(true);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public JournalReportResponse journal(UUID userId, LocalDate from, LocalDate to, TransactionType type,
                                         Long categoryId, int page, int size) {
        User user = loadUser(userId);
        JournalFilter f = filter(user, from, to, type, categoryId);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        BigDecimal opening = queries.openingBalance(userId, f.from());
        JournalSummaryResponse summary = summary(f, opening);
        List<JournalEntryResponse> items = queries.page(f, opening, safePage, safeSize);
        int totalPages = (int) ((summary.entryCount() + safeSize - 1) / safeSize);
        return new JournalReportResponse(f.from(), f.to(), f.filtered(), summary,
                new PageResponse<>(items, safePage, safeSize, summary.entryCount(), totalPages));
    }

    /**
     * Validasi & batas dijalankan SEBELUM response mulai di-stream, agar error bisa dikirim sebagai JSON biasa.
     */
    @Transactional(readOnly = true)
    public PreparedExport prepareExport(UUID userId, LocalDate from, LocalDate to, TransactionType type, Long categoryId) {
        rateLimiter.consume("report-export:" + userId, 10, Duration.ofMinutes(10));
        User user = loadUser(userId);
        JournalFilter f = filter(user, from, to, type, categoryId);
        long count = queries.count(f);
        if (count > MAX_EXPORT_ROWS) {
            throw new ApiException(ErrorCode.REPORT_TOO_LARGE,
                    "Maksimal " + MAX_EXPORT_ROWS + " transaksi per unduhan, persempit periodenya");
        }
        String filename = "sakutahu-jurnal-" + f.from() + "_" + f.to() + ".xlsx";
        return new PreparedExport(f, user.getFullName(), user.getTimezone(), filename);
    }

    /** Menulis .xlsx ke output (dipanggil dari StreamingResponseBody, di luar transaksi request). */
    public void writeExport(PreparedExport export, OutputStream out) throws IOException {
        IOException[] failure = new IOException[1];
        readOnlyTx.executeWithoutResult(status -> {
            JournalFilter f = export.filter();
            BigDecimal opening = queries.openingBalance(f.userId(), f.from());
            JournalSummaryResponse summary = summary(f, opening);
            List<CategoryRecapResponse> recap = recap(f);
            try {
                exporter.write(out, export, summary, recap, consumer -> queries.stream(f, opening, consumer));
            } catch (IOException e) {
                failure[0] = e;
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
    }

    private JournalSummaryResponse summary(JournalFilter f, BigDecimal opening) {
        Map<CategoryKind, BigDecimal> totals = queries.totalsByKind(f);
        BigDecimal income = totals.get(CategoryKind.INCOME);
        BigDecimal expense = totals.get(CategoryKind.SPENDING);
        BigDecimal saving = totals.get(CategoryKind.SAVING);
        long count = queries.count(f);
        if (f.filtered()) {
            return new JournalSummaryResponse(null, income, expense, saving, null, count);
        }
        BigDecimal closing = opening.add(income).subtract(expense).subtract(saving);
        return new JournalSummaryResponse(opening, income, expense, saving, closing, count);
    }

    List<CategoryRecapResponse> recap(JournalFilter f) {
        List<JournalQueryRepository.CategoryTotal> rows = queries.categoryTotals(f);
        Map<CategoryKind, BigDecimal> kindTotals = queries.totalsByKind(f);
        return rows.stream().map(r -> {
            BigDecimal kindTotal = kindTotals.getOrDefault(r.kind(), BigDecimal.ZERO);
            double share = kindTotal.signum() == 0 ? 0
                    : r.total().divide(kindTotal, 6, RoundingMode.HALF_UP).doubleValue();
            return new CategoryRecapResponse(r.id(), r.name(), r.icon(), r.kind(), r.count(), r.total(), share);
        }).toList();
    }

    private JournalFilter filter(User user, LocalDate from, LocalDate to, TransactionType type, Long categoryId) {
        // Default: bulan berjalan menurut zona waktu user (sama dengan dashboard)
        if (from == null || to == null) {
            LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(user.getTimezone())));
            from = today.withDayOfMonth(1);
            to = today.with(TemporalAdjusters.lastDayOfMonth());
        }
        if (from.isAfter(to)) {
            throw new ApiException(ErrorCode.REPORT_RANGE_INVALID, "Tanggal awal tidak boleh setelah tanggal akhir");
        }
        if (!to.isBefore(from.plusMonths(MAX_RANGE_MONTHS))) {
            throw new ApiException(ErrorCode.REPORT_RANGE_INVALID, "Rentang laporan maksimal " + MAX_RANGE_MONTHS + " bulan");
        }
        return new JournalFilter(user.getId(), from, to, type, categoryId);
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }

    /** Hasil validasi export: filter, data kop laporan, dan nama file. */
    public record PreparedExport(JournalFilter filter, String fullName, String timezone, String filename) {}
}
