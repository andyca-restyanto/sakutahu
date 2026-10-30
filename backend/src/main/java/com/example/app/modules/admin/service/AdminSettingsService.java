// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminSettingsService.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.dto.ReceiptScanSettingsResponse;
import com.example.app.modules.admin.dto.ReceiptScanUsage;
import com.example.app.modules.admin.dto.UpdateReceiptScanSettingsRequest;
import com.example.app.modules.subscription.entity.FeatureAccessMode;
import com.example.app.modules.subscription.service.FeatureAccessService;
import com.example.app.shared.settings.AppSettingsService;
import com.example.app.shared.util.ClientContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Menu "Kuota Struk": akses fitur baca struk & kuota user FREE per bulan. */
@Service
public class AdminSettingsService {

    /** Statistik bulanan dihitung dengan zona waktu ini (kuota per user tetap memakai zona waktu user). */
    static final ZoneId STATS_ZONE = ZoneId.of("Asia/Jakarta");

    private final AppSettingsService settings;
    private final FeatureAccessService featureAccessService;
    private final AdminAuditService auditService;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public AdminSettingsService(AppSettingsService settings, FeatureAccessService featureAccessService,
                                AdminAuditService auditService, NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.settings = settings;
        this.featureAccessService = featureAccessService;
        this.auditService = auditService;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReceiptScanSettingsResponse receiptScan() {
        settings.invalidate(); // halaman admin selalu menampilkan nilai terbaru dari database
        FeatureAccessMode access = featureAccessService.accessMode();
        int quota = featureAccessService.freeQuota();
        return new ReceiptScanSettingsResponse(access, quota, updatedAt(), usage(access, quota));
    }

    @Transactional
    public ReceiptScanSettingsResponse updateReceiptScan(UUID adminId, UpdateReceiptScanSettingsRequest request,
                                                         ClientContext ctx) {
        settings.invalidate();
        FeatureAccessMode beforeAccess = featureAccessService.accessMode();
        int beforeQuota = featureAccessService.freeQuota();

        settings.set(FeatureAccessService.RECEIPT_ACCESS_KEY, request.access().name());
        settings.set(FeatureAccessService.RECEIPT_FREE_QUOTA_KEY, String.valueOf(request.freeMonthlyQuota()));

        if (beforeAccess != request.access() || beforeQuota != request.freeMonthlyQuota()) {
            auditService.record(adminId, "RECEIPT_SETTINGS_CHANGED", AdminAuditService.TARGET_SETTINGS, "receipt_scan",
                    AdminAuditService.detail("fromAccess", beforeAccess, "fromQuota", beforeQuota,
                            "toAccess", request.access(), "toQuota", request.freeMonthlyQuota()), ctx.ip());
        }
        return new ReceiptScanSettingsResponse(request.access(), request.freeMonthlyQuota(), updatedAt(),
                usage(request.access(), request.freeMonthlyQuota()));
    }

    private Instant updatedAt() {
        Timestamp ts = jdbc.queryForObject("SELECT MAX(updated_at) FROM app_settings WHERE key IN (:keys)",
                new MapSqlParameterSource("keys", List.of(FeatureAccessService.RECEIPT_ACCESS_KEY,
                        FeatureAccessService.RECEIPT_FREE_QUOTA_KEY)), Timestamp.class);
        return ts == null ? null : ts.toInstant();
    }

    private ReceiptScanUsage usage(FeatureAccessMode access, int quota) {
        LocalDate firstDay = LocalDate.now(clock.withZone(STATS_ZONE)).withDayOfMonth(1);
        Instant from = firstDay.atStartOfDay(STATS_ZONE).toInstant();
        Instant now = clock.instant();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("now", Timestamp.from(now))
                .addValue("quota", quota);

        Map<String, Object> counts = jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE status = 'DONE') AS success,
                       COUNT(*) FILTER (WHERE status = 'FAILED') AS failed
                FROM receipt_scans WHERE created_at >= :from
                """, params);

        int usersAtQuota = 0;
        if (access == FeatureAccessMode.ALL) {
            String freeUser = "NOT (u.plan = 'VIP' AND (u.plan_expires_at IS NULL OR u.plan_expires_at > :now))";
            Integer n = quota <= 0
                    ? jdbc.queryForObject("SELECT COUNT(*) FROM users u WHERE " + freeUser, params, Integer.class)
                    // Disusun dengan String.join: spasi di akhir baris text block dibuang Java,
                    // sehingga "AND " + freeUser sempat menjadi "ANDNOT (...)"
                    : jdbc.queryForObject(String.join("\n",
                            "SELECT COUNT(*) FROM (",
                            "  SELECT r.user_id FROM receipt_scans r JOIN users u ON u.id = r.user_id",
                            "  WHERE r.created_at >= :from AND r.status <> 'FAILED' AND " + freeUser,
                            "  GROUP BY r.user_id HAVING COUNT(*) >= :quota",
                            ") x"), params, Integer.class);
            usersAtQuota = n == null ? 0 : n;
        }
        return new ReceiptScanUsage(firstDay.toString().substring(0, 7),
                ((Number) counts.get("total")).intValue(),
                ((Number) counts.get("success")).intValue(),
                ((Number) counts.get("failed")).intValue(),
                usersAtQuota);
    }
}
