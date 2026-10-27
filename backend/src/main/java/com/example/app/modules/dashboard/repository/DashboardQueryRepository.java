// filepath: /backend/src/main/java/com/example/app/modules/dashboard/repository/DashboardQueryRepository.java
package com.example.app.modules.dashboard.repository;

import com.example.app.modules.category.entity.CategoryKind;
import com.example.app.modules.dashboard.dto.Granularity;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Query agregasi baca-saja untuk dashboard (native SQL, tanpa entity).
 * Semua query WAJIB difilter user_id agar data user lain tidak pernah ikut terhitung.
 */
@Repository
public class DashboardQueryRepository {

    public record CategoryTotalRow(Long categoryId, String code, String name, String color, String icon,
                                   CategoryKind kind, BigDecimal amount, long count) {}

    public record TrendRow(LocalDate bucket, BigDecimal income, BigDecimal spending, BigDecimal saving) {}

    private static final String TOTALS_SQL = """
            SELECT c.kind AS kind, COALESCE(SUM(t.amount), 0) AS total
            FROM transactions t
            JOIN categories c ON c.id = t.category_id
            WHERE t.user_id = :userId AND t.occurred_on BETWEEN :from AND :to
            GROUP BY c.kind
            """;

    private static final String BY_CATEGORY_SQL = """
            SELECT c.id, c.code, c.name, c.color, c.icon, c.kind,
                   SUM(t.amount) AS amount, COUNT(*) AS cnt
            FROM transactions t
            JOIN categories c ON c.id = t.category_id
            WHERE t.user_id = :userId AND t.occurred_on BETWEEN :from AND :to
            GROUP BY c.id, c.code, c.name, c.color, c.icon, c.kind, c.sort_order
            ORDER BY amount DESC, c.sort_order
            """;

    /** generate_series mengisi hari tanpa transaksi dengan 0 agar grafik tidak bolong. */
    private static final String TREND_DAY_SQL = """
            SELECT CAST(b.bucket AS date) AS bucket,
                   COALESCE(SUM(t.amount) FILTER (WHERE c.kind = 'INCOME'), 0)   AS income,
                   COALESCE(SUM(t.amount) FILTER (WHERE c.kind = 'SPENDING'), 0) AS spending,
                   COALESCE(SUM(t.amount) FILTER (WHERE c.kind = 'SAVING'), 0)   AS saving
            FROM generate_series(CAST(:from AS timestamp), CAST(:to AS timestamp), INTERVAL '1 day') AS b(bucket)
            LEFT JOIN transactions t
                   ON t.user_id = :userId AND t.occurred_on = CAST(b.bucket AS date)
            LEFT JOIN categories c ON c.id = t.category_id
            GROUP BY b.bucket
            ORDER BY b.bucket
            """;

    private static final String TREND_MONTH_SQL = """
            SELECT CAST(b.bucket AS date) AS bucket,
                   COALESCE(SUM(t.amount) FILTER (WHERE c.kind = 'INCOME'), 0)   AS income,
                   COALESCE(SUM(t.amount) FILTER (WHERE c.kind = 'SPENDING'), 0) AS spending,
                   COALESCE(SUM(t.amount) FILTER (WHERE c.kind = 'SAVING'), 0)   AS saving
            FROM generate_series(date_trunc('month', CAST(:from AS timestamp)),
                                 date_trunc('month', CAST(:to AS timestamp)),
                                 INTERVAL '1 month') AS b(bucket)
            LEFT JOIN transactions t
                   ON t.user_id = :userId
                  AND t.occurred_on BETWEEN :from AND :to
                  AND date_trunc('month', CAST(t.occurred_on AS timestamp)) = b.bucket
            LEFT JOIN categories c ON c.id = t.category_id
            GROUP BY b.bucket
            ORDER BY b.bucket
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<CategoryKind, BigDecimal> totalsByKind(UUID userId, LocalDate from, LocalDate to) {
        Map<CategoryKind, BigDecimal> totals = new EnumMap<>(CategoryKind.class);
        for (CategoryKind kind : CategoryKind.values()) {
            totals.put(kind, BigDecimal.ZERO);
        }
        jdbc.query(TOTALS_SQL, params(userId, from, to), (RowCallbackHandler) rs -> {
            totals.put(CategoryKind.valueOf(rs.getString("kind")), rs.getBigDecimal("total"));
        });
        return totals;
    }

    public List<CategoryTotalRow> totalsByCategory(UUID userId, LocalDate from, LocalDate to) {
        return jdbc.query(BY_CATEGORY_SQL, params(userId, from, to), (rs, i) -> new CategoryTotalRow(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("color"),
                rs.getString("icon"),
                CategoryKind.valueOf(rs.getString("kind")),
                rs.getBigDecimal("amount"),
                rs.getLong("cnt")));
    }

    public List<TrendRow> trend(UUID userId, LocalDate from, LocalDate to, Granularity granularity) {
        String sql = granularity == Granularity.DAY ? TREND_DAY_SQL : TREND_MONTH_SQL;
        return jdbc.query(sql, params(userId, from, to), (rs, i) -> new TrendRow(
                rs.getObject("bucket", LocalDate.class),
                rs.getBigDecimal("income"),
                rs.getBigDecimal("spending"),
                rs.getBigDecimal("saving")));
    }

    private static MapSqlParameterSource params(UUID userId, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("from", from)
                .addValue("to", to);
    }
}
