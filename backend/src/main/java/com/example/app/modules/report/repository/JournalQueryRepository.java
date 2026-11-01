// filepath: /backend/src/main/java/com/example/app/modules/report/repository/JournalQueryRepository.java
package com.example.app.modules.report.repository;

import com.example.app.modules.category.dto.CategorySummaryResponse;
import com.example.app.modules.category.entity.CategoryKind;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.report.dto.JournalEntryResponse;
import com.example.app.modules.report.service.JournalFilter;
import com.example.app.modules.transaction.entity.TransactionSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Query buku kas. Saldo berjalan dihitung database dengan window function (urut tanggal → waktu dicatat → id),
 * sehingga angka di layar, di Excel, dan di dashboard selalu sama.
 * Rumus saldo: pemasukan − pengeluaran − tabungan (tabungan = transaksi EXPENSE berkategori SAVING).
 */
@Repository
public class JournalQueryRepository {

    /** +amount untuk pemasukan, −amount untuk pengeluaran & tabungan. */
    static final String SIGNED = "CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END";
    static final String ORDER = "t.occurred_on, t.created_at, t.id";

    private final NamedParameterJdbcTemplate jdbc;
    /** Untuk export: membaca bertahap (fetch size) tanpa memuat semua baris ke memori. */
    private final NamedParameterJdbcTemplate streamingJdbc;

    public JournalQueryRepository(NamedParameterJdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        JdbcTemplate streaming = new JdbcTemplate(dataSource);
        streaming.setFetchSize(1000);
        this.streamingJdbc = new NamedParameterJdbcTemplate(streaming);
    }

    /** Saldo bersih semua transaksi user SEBELUM periode (tanpa filter jenis/kategori). */
    public BigDecimal openingBalance(UUID userId, java.time.LocalDate from) {
        BigDecimal v = jdbc.queryForObject(
                "SELECT COALESCE(SUM(" + SIGNED + "), 0) FROM transactions t WHERE t.user_id = :userId AND t.occurred_on < :from",
                new MapSqlParameterSource().addValue("userId", userId).addValue("from", Date.valueOf(from)),
                BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Total periode per kind (INCOME / SPENDING / SAVING), mengikuti filter. */
    public Map<CategoryKind, BigDecimal> totalsByKind(JournalFilter f) {
        Map<CategoryKind, BigDecimal> totals = new EnumMap<>(CategoryKind.class);
        for (CategoryKind k : CategoryKind.values()) {
            totals.put(k, BigDecimal.ZERO);
        }
        MapSqlParameterSource p = params(f);
        jdbc.query("SELECT c.kind, COALESCE(SUM(t.amount), 0) AS total FROM transactions t "
                        + "JOIN categories c ON c.id = t.category_id" + where(f, p) + " GROUP BY c.kind", p,
                (RowCallbackHandler) rs -> totals.put(CategoryKind.valueOf(rs.getString("kind")), rs.getBigDecimal("total")));
        return totals;
    }

    public long count(JournalFilter f) {
        MapSqlParameterSource p = params(f);
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM transactions t" + where(f, p), p, Long.class);
        return n == null ? 0 : n;
    }

    public List<JournalEntryResponse> page(JournalFilter f, BigDecimal opening, int page, int size) {
        MapSqlParameterSource p = params(f).addValue("opening", opening)
                .addValue("limit", size).addValue("offset", (long) page * size);
        return jdbc.query(entriesSql(f, p) + " LIMIT :limit OFFSET :offset", p, ENTRY_MAPPER);
    }

    /** Semua baris periode untuk export, satu per satu (urut sama dengan layar). */
    public void stream(JournalFilter f, BigDecimal opening, Consumer<JournalEntryResponse> consumer) {
        MapSqlParameterSource p = params(f).addValue("opening", opening);
        int[] row = {0};
        streamingJdbc.query(entriesSql(f, p), p, (RowCallbackHandler) rs -> consumer.accept(ENTRY_MAPPER.mapRow(rs, row[0]++)));
    }

    /** Rekap per kategori (jumlah transaksi & total) untuk sheet Ringkasan. */
    public List<CategoryTotal> categoryTotals(JournalFilter f) {
        MapSqlParameterSource p = params(f);
        return jdbc.query("SELECT c.id, c.name, c.icon, c.kind, COUNT(*) AS n, SUM(t.amount) AS total, MIN(c.sort_order) AS so "
                        + "FROM transactions t JOIN categories c ON c.id = t.category_id" + where(f, p)
                        + " GROUP BY c.id, c.name, c.icon, c.kind ORDER BY c.kind, total DESC, so", p,
                (rs, i) -> new CategoryTotal(rs.getLong("id"), rs.getString("name"), rs.getString("icon"),
                        CategoryKind.valueOf(rs.getString("kind")), rs.getLong("n"), rs.getBigDecimal("total")));
    }

    public record CategoryTotal(Long id, String name, String icon, CategoryKind kind, long count, BigDecimal total) {}

    // ---------- SQL ----------

    private static String entriesSql(JournalFilter f, MapSqlParameterSource p) {
        // Saldo berjalan hanya untuk laporan tanpa filter; window dihitung SEBELUM LIMIT/OFFSET
        String balance = f.filtered()
                ? "CAST(NULL AS NUMERIC)"
                : ":opening + SUM(" + SIGNED + ") OVER (ORDER BY " + ORDER + " ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)";
        return "SELECT t.id, t.occurred_on, t.note, t.type, t.source, t.receipt_scan_id, t.amount, t.created_at, "
                + "c.id AS c_id, c.code AS c_code, c.name AS c_name, c.kind AS c_kind, c.color AS c_color, c.icon AS c_icon, "
                + balance + " AS balance "
                + "FROM transactions t JOIN categories c ON c.id = t.category_id" + where(f, p)
                + " ORDER BY " + ORDER;
    }

    private static MapSqlParameterSource params(JournalFilter f) {
        return new MapSqlParameterSource()
                .addValue("userId", f.userId())
                .addValue("from", Date.valueOf(f.from()))
                .addValue("to", Date.valueOf(f.to()));
    }

    private static String where(JournalFilter f, MapSqlParameterSource p) {
        StringBuilder sb = new StringBuilder(" WHERE t.user_id = :userId AND t.occurred_on BETWEEN :from AND :to");
        if (f.type() != null) {
            sb.append(" AND t.type = :type");
            p.addValue("type", f.type().name());
        }
        if (f.categoryId() != null) {
            sb.append(" AND t.category_id = :categoryId");
            p.addValue("categoryId", f.categoryId());
        }
        return sb.toString();
    }

    private static final RowMapper<JournalEntryResponse> ENTRY_MAPPER = JournalQueryRepository::mapEntry;

    private static JournalEntryResponse mapEntry(ResultSet rs, int i) throws SQLException {
        TransactionType type = TransactionType.valueOf(rs.getString("type"));
        BigDecimal amount = rs.getBigDecimal("amount");
        String note = rs.getString("note");
        String categoryName = rs.getString("c_name");
        return new JournalEntryResponse(
                rs.getObject("id", UUID.class),
                rs.getDate("occurred_on").toLocalDate(),
                note == null || note.isBlank() ? categoryName : note,
                new CategorySummaryResponse(rs.getLong("c_id"), rs.getString("c_code"), categoryName,
                        CategoryKind.valueOf(rs.getString("c_kind")), rs.getString("c_color"), rs.getString("c_icon")),
                type,
                TransactionSource.valueOf(rs.getString("source")),
                rs.getObject("receipt_scan_id", UUID.class),
                type == TransactionType.INCOME ? amount : BigDecimal.ZERO,
                type == TransactionType.EXPENSE ? amount : BigDecimal.ZERO,
                rs.getBigDecimal("balance"),
                rs.getTimestamp("created_at").toInstant());
    }
}
