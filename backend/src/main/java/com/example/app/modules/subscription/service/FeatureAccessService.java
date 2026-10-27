// filepath: /backend/src/main/java/com/example/app/modules/subscription/service/FeatureAccessService.java
package com.example.app.modules.subscription.service;

import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.subscription.dto.FeatureAccessResponse;
import com.example.app.modules.subscription.dto.ReceiptScanAccess;
import com.example.app.modules.subscription.entity.FeatureAccessMode;
import com.example.app.modules.subscription.entity.FeatureDenyReason;
import com.example.app.modules.subscription.entity.UserPlan;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.settings.AppSettingsService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

/**
 * Menentukan apakah user boleh memakai fitur berbayar.
 * Pengaturan dari app_settings: receipt_scan.access (ALL | VIP_ONLY) & receipt_scan.free_monthly_quota.
 */
@Service
public class FeatureAccessService {

    public static final String RECEIPT_ACCESS_KEY = "receipt_scan.access";
    public static final String RECEIPT_FREE_QUOTA_KEY = "receipt_scan.free_monthly_quota";
    static final int DEFAULT_FREE_QUOTA = 3;

    private final UserRepository userRepository;
    private final PlanService planService;
    private final AppSettingsService settings;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public FeatureAccessService(UserRepository userRepository, PlanService planService, AppSettingsService settings,
                                JdbcTemplate jdbc, Clock clock) {
        this.userRepository = userRepository;
        this.planService = planService;
        this.settings = settings;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FeatureAccessResponse features(UUID userId) {
        User user = load(userId);
        return new FeatureAccessResponse(planService.effectivePlan(user), user.getPlanExpiresAt(), receiptScan(user));
    }

    @Transactional(readOnly = true)
    public ReceiptScanAccess receiptScan(UUID userId) {
        return receiptScan(load(userId));
    }

    ReceiptScanAccess receiptScan(User user) {
        UserPlan plan = planService.effectivePlan(user);
        FeatureAccessMode mode = accessMode();
        int used = usedThisMonth(user);
        return decide(plan, mode, freeQuota(), used);
    }

    /** Logika murni (tanpa I/O) agar mudah dites. */
    static ReceiptScanAccess decide(UserPlan plan, FeatureAccessMode mode, int freeQuota, int used) {
        if (plan == UserPlan.VIP) {
            return new ReceiptScanAccess(true, mode, null, null, used);
        }
        if (mode == FeatureAccessMode.VIP_ONLY) {
            return new ReceiptScanAccess(false, mode, FeatureDenyReason.VIP_ONLY, 0, used);
        }
        boolean available = used < freeQuota;
        return new ReceiptScanAccess(available, mode, available ? null : FeatureDenyReason.QUOTA_EXCEEDED, freeQuota, used);
    }

    FeatureAccessMode accessMode() {
        String raw = settings.getString(RECEIPT_ACCESS_KEY, FeatureAccessMode.VIP_ONLY.name());
        try {
            return FeatureAccessMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return FeatureAccessMode.VIP_ONLY; // nilai tak dikenal → paling aman
        }
    }

    int freeQuota() {
        return Math.max(0, settings.getInt(RECEIPT_FREE_QUOTA_KEY, DEFAULT_FREE_QUOTA));
    }

    /**
     * Struk yang dipakai bulan ini (bulan kalender menurut zona waktu user).
     * Scan FAILED (kesalahan sistem / foto tidak terbaca) tidak memotong kuota.
     */
    int usedThisMonth(User user) {
        ZoneId zone = ZoneId.of(user.getTimezone());
        LocalDate firstDay = LocalDate.now(clock.withZone(zone)).withDayOfMonth(1);
        Instant from = firstDay.atStartOfDay(zone).toInstant();
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM receipt_scans WHERE user_id = ? AND created_at >= ? AND status <> 'FAILED'",
                Integer.class, user.getId(), Timestamp.from(from));
        return count == null ? 0 : count;
    }

    private User load(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }
}
