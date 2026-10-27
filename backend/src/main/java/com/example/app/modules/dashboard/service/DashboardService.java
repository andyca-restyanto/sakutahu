// filepath: /backend/src/main/java/com/example/app/modules/dashboard/service/DashboardService.java
package com.example.app.modules.dashboard.service;

import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.category.entity.CategoryKind;
import com.example.app.modules.dashboard.dto.CategoryBreakdownItem;
import com.example.app.modules.dashboard.dto.DashboardByCategory;
import com.example.app.modules.dashboard.dto.DashboardComparison;
import com.example.app.modules.dashboard.dto.DashboardPeriod;
import com.example.app.modules.dashboard.dto.DashboardResponse;
import com.example.app.modules.dashboard.dto.DashboardSummary;
import com.example.app.modules.dashboard.dto.Granularity;
import com.example.app.modules.dashboard.dto.TrendPoint;
import com.example.app.modules.dashboard.repository.DashboardQueryRepository;
import com.example.app.modules.dashboard.repository.DashboardQueryRepository.CategoryTotalRow;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    static final int MAX_RANGE_MONTHS = 24;
    static final int MAX_DAILY_DAYS = 31;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final DashboardQueryRepository queries;
    private final UserRepository userRepository;
    private final Clock clock;

    public DashboardService(DashboardQueryRepository queries, UserRepository userRepository, Clock clock) {
        this.queries = queries;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(UUID userId, LocalDate from, LocalDate to) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));

        // Default: bulan berjalan menurut zona waktu user
        if (from == null || to == null) {
            LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(user.getTimezone())));
            from = today.withDayOfMonth(1);
            to = today.with(TemporalAdjusters.lastDayOfMonth());
        }
        validateRange(from, to);

        Granularity granularity = granularityOf(from, to);
        LocalDate[] previous = previousPeriod(from, to);

        Map<CategoryKind, BigDecimal> current = queries.totalsByKind(userId, from, to);
        Map<CategoryKind, BigDecimal> before = queries.totalsByKind(userId, previous[0], previous[1]);

        BigDecimal income = current.get(CategoryKind.INCOME);
        BigDecimal spending = current.get(CategoryKind.SPENDING);
        BigDecimal saving = current.get(CategoryKind.SAVING);
        DashboardSummary summary = new DashboardSummary(
                income, spending, saving,
                income.subtract(spending).subtract(saving),
                percentOf(saving, income));

        DashboardComparison comparison = new DashboardComparison(
                change(income, before.get(CategoryKind.INCOME)),
                change(spending, before.get(CategoryKind.SPENDING)),
                change(saving, before.get(CategoryKind.SAVING)));

        List<CategoryTotalRow> rows = queries.totalsByCategory(userId, from, to);
        DashboardByCategory byCategory = new DashboardByCategory(
                breakdown(rows, CategoryKind.SPENDING, spending),
                breakdown(rows, CategoryKind.INCOME, income));

        List<TrendPoint> trend = queries.trend(userId, from, to, granularity).stream()
                .map(r -> new TrendPoint(
                        granularity == Granularity.DAY ? r.bucket().toString() : YearMonth.from(r.bucket()).toString(),
                        r.income(), r.spending(), r.saving()))
                .toList();

        DashboardPeriod period = new DashboardPeriod(from, to, previous[0], previous[1], granularity, user.getCurrency());
        return new DashboardResponse(period, summary, comparison, byCategory, trend);
    }

    static void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tanggal awal tidak boleh setelah tanggal akhir");
        }
        if (!to.isBefore(from.plusMonths(MAX_RANGE_MONTHS))) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Rentang maksimal " + MAX_RANGE_MONTHS + " bulan");
        }
    }

    static Granularity granularityOf(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return days <= MAX_DAILY_DAYS ? Granularity.DAY : Granularity.MONTH;
    }

    /**
     * Periode pembanding dengan panjang yang sama, tepat sebelum periode ini.
     * Jika periode berupa bulan penuh (1 Okt–31 Okt), pembandingnya juga bulan penuh (1 Sep–30 Sep).
     */
    static LocalDate[] previousPeriod(LocalDate from, LocalDate to) {
        boolean fullMonths = from.getDayOfMonth() == 1 && to.equals(to.with(TemporalAdjusters.lastDayOfMonth()));
        if (fullMonths) {
            long months = ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to)) + 1;
            LocalDate prevFrom = from.minusMonths(months);
            return new LocalDate[]{prevFrom, from.minusDays(1)};
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        return new LocalDate[]{prevTo.minusDays(days - 1), prevTo};
    }

    /** Persentase perubahan (current - previous) / previous × 100, 1 desimal; null jika previous 0. */
    static BigDecimal change(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(HUNDRED).divide(previous, 1, RoundingMode.HALF_UP);
    }

    static BigDecimal percentOf(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(total, 1, RoundingMode.HALF_UP);
    }

    private static List<CategoryBreakdownItem> breakdown(List<CategoryTotalRow> rows, CategoryKind kind, BigDecimal total) {
        return rows.stream()
                .filter(r -> r.kind() == kind)
                .map(r -> new CategoryBreakdownItem(
                        r.categoryId(), r.code(), r.name(), r.color(), r.icon(), r.amount(),
                        total.signum() == 0 ? BigDecimal.ZERO : percentOf(r.amount(), total),
                        r.count()))
                .toList();
    }
}
