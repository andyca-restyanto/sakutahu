// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminUserService.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.dto.AdminAuditEntryResponse;
import com.example.app.modules.admin.dto.AdminUserDetailResponse;
import com.example.app.modules.admin.dto.AdminUserStatusFilter;
import com.example.app.modules.admin.dto.AdminUserSummaryResponse;
import com.example.app.modules.admin.dto.BlockUserRequest;
import com.example.app.modules.admin.dto.ChangePlanRequest;
import com.example.app.modules.admin.entity.AdminAuditLog;
import com.example.app.modules.admin.entity.AdminUser;
import com.example.app.modules.admin.repository.AdminAuditLogRepository;
import com.example.app.modules.admin.repository.AdminUserRepository;
import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.RefreshTokenRepository;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.modules.subscription.entity.UserPlan;
import com.example.app.modules.subscription.service.FeatureAccessService;
import com.example.app.modules.subscription.service.PlanService;
import com.example.app.modules.telegram.entity.TelegramAccount;
import com.example.app.modules.telegram.repository.TelegramAccountRepository;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.security.BlockedUserChecker;
import com.example.app.shared.util.ClientContext;
import com.example.app.shared.web.PageResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Menu User di panel admin: lihat, ubah plan, blokir/buka blokir. Isi transaksi user tidak diakses. */
@Service
public class AdminUserService {

    static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TelegramAccountRepository telegramAccountRepository;
    private final AdminUserRepository adminRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final AdminAuditService auditService;
    private final PlanService planService;
    private final FeatureAccessService featureAccessService;
    private final BlockedUserChecker blockedUserChecker;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public AdminUserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                            TelegramAccountRepository telegramAccountRepository, AdminUserRepository adminRepository,
                            AdminAuditLogRepository auditLogRepository, AdminAuditService auditService,
                            PlanService planService, FeatureAccessService featureAccessService,
                            BlockedUserChecker blockedUserChecker, NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.telegramAccountRepository = telegramAccountRepository;
        this.adminRepository = adminRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.planService = planService;
        this.featureAccessService = featureAccessService;
        this.blockedUserChecker = blockedUserChecker;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummaryResponse> list(String q, UserPlan plan, AdminUserStatusFilter status,
                                                       int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Instant now = clock.instant();

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("now", Timestamp.from(now));
        if (q != null && !q.isBlank()) {
            where.append(" AND (u.email ILIKE :q ESCAPE '\\' OR u.full_name ILIKE :q ESCAPE '\\')");
            params.addValue("q", "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%");
        }
        String vipNow = "(u.plan = 'VIP' AND (u.plan_expires_at IS NULL OR u.plan_expires_at > :now))";
        if (plan == UserPlan.VIP) {
            where.append(" AND ").append(vipNow);
        } else if (plan == UserPlan.FREE) {
            where.append(" AND NOT ").append(vipNow);
        }
        if (status == AdminUserStatusFilter.BLOCKED) {
            where.append(" AND u.blocked_at IS NOT NULL");
        } else if (status == AdminUserStatusFilter.ACTIVE) {
            where.append(" AND u.blocked_at IS NULL");
        }

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM users u" + where, params, Long.class);
        long totalItems = total == null ? 0 : total;

        params.addValue("limit", safeSize).addValue("offset", (long) safePage * safeSize);
        List<AdminUserSummaryResponse> items = jdbc.query("""
                SELECT u.id, u.full_name, u.email, u.plan, u.plan_expires_at, u.email_verified_at, u.blocked_at,
                       u.created_at, EXISTS (SELECT 1 FROM telegram_accounts t WHERE t.user_id = u.id) AS tg
                FROM users u""" + where + " ORDER BY u.created_at DESC, u.id LIMIT :limit OFFSET :offset",
                params, (rs, i) -> {
                    UserPlan stored = UserPlan.valueOf(rs.getString("plan"));
                    Timestamp expires = rs.getTimestamp("plan_expires_at");
                    Instant expiresAt = expires == null ? null : expires.toInstant();
                    UserPlan effective = stored == UserPlan.VIP && (expiresAt == null || expiresAt.isAfter(now))
                            ? UserPlan.VIP : UserPlan.FREE;
                    return new AdminUserSummaryResponse(
                            rs.getObject("id", UUID.class),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            effective,
                            expiresAt,
                            rs.getTimestamp("email_verified_at") != null,
                            rs.getBoolean("tg"),
                            rs.getTimestamp("blocked_at") != null,
                            rs.getTimestamp("created_at").toInstant());
                });
        int totalPages = (int) ((totalItems + safeSize - 1) / safeSize);
        return new PageResponse<>(items, safePage, safeSize, totalItems, totalPages);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse detail(UUID userId) {
        return toDetail(load(userId));
    }

    @Transactional
    public AdminUserDetailResponse changePlan(UUID adminId, UUID userId, ChangePlanRequest request, ClientContext ctx) {
        User user = load(userId);
        Instant expiresAt = request.plan() == UserPlan.VIP ? request.planExpiresAt() : null;
        if (expiresAt != null && !expiresAt.isAfter(clock.instant())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tanggal berakhir VIP harus di masa depan");
        }
        UserPlan beforePlan = planService.effectivePlan(user);
        Instant beforeExpires = user.getPlanExpiresAt();
        user.changePlan(request.plan(), expiresAt);
        userRepository.saveAndFlush(user);
        auditService.record(adminId, "USER_PLAN_CHANGED", AdminAuditService.TARGET_USER, userId,
                AdminAuditService.detail("fromPlan", beforePlan, "fromExpiresAt", beforeExpires,
                        "toPlan", request.plan(), "toExpiresAt", expiresAt), ctx.ip());
        return toDetail(user);
    }

    /** Memblokir: semua sesi web dicabut, API & bot Telegram langsung menolak user ini. */
    @Transactional
    public AdminUserDetailResponse block(UUID adminId, UUID userId, BlockUserRequest request, ClientContext ctx) {
        User user = load(userId);
        if (!user.isBlocked()) {
            Instant now = clock.instant();
            user.block(request.reason().trim(), adminId, now);
            userRepository.saveAndFlush(user);
            refreshTokenRepository.revokeAllActiveForUser(userId, now);
            auditService.record(adminId, "USER_BLOCKED", AdminAuditService.TARGET_USER, userId,
                    AdminAuditService.detail("reason", user.getBlockedReason()), ctx.ip());
            evictAfterCommit(userId);
        }
        return toDetail(user);
    }

    @Transactional
    public AdminUserDetailResponse unblock(UUID adminId, UUID userId, ClientContext ctx) {
        User user = load(userId);
        if (user.isBlocked()) {
            String reason = user.getBlockedReason();
            user.unblock();
            userRepository.saveAndFlush(user);
            auditService.record(adminId, "USER_UNBLOCKED", AdminAuditService.TARGET_USER, userId,
                    AdminAuditService.detail("previousReason", reason), ctx.ip());
            evictAfterCommit(userId);
        }
        return toDetail(user);
    }

    private AdminUserDetailResponse toDetail(User user) {
        Optional<TelegramAccount> tg = telegramAccountRepository.findByUserId(user.getId());
        Long txCount = jdbc.queryForObject("SELECT COUNT(*) FROM transactions WHERE user_id = :id",
                new MapSqlParameterSource("id", user.getId()), Long.class);
        return new AdminUserDetailResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                planService.effectivePlan(user),
                user.getPlanExpiresAt(),
                user.isEmailVerified(),
                tg.isPresent(),
                tg.map(TelegramAccount::getUsername).orElse(null),
                user.getTimezone(),
                txCount == null ? 0 : txCount,
                featureAccessService.usedThisMonth(user),
                user.isBlocked(),
                user.getBlockedAt(),
                user.getBlockedReason(),
                user.getCreatedAt(),
                history(user.getId()));
    }

    private List<AdminAuditEntryResponse> history(UUID userId) {
        List<AdminAuditLog> logs = auditLogRepository.findTop20ByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                AdminAuditService.TARGET_USER, userId.toString());
        Map<UUID, String> names = adminRepository.findAllById(
                        logs.stream().map(AdminAuditLog::getAdminId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(AdminUser::getId, AdminUser::getFullName));
        List<AdminAuditEntryResponse> result = new ArrayList<>();
        for (AdminAuditLog log : logs) {
            result.add(new AdminAuditEntryResponse(log.getId(), log.getAction(),
                    log.getAdminId() == null ? null : names.get(log.getAdminId()), log.getDetail(), log.getCreatedAt()));
        }
        return result;
    }

    /** Cache blokir dibuang setelah commit, supaya request berikutnya membaca status terbaru. */
    private void evictAfterCommit(UUID userId) {
        blockedUserChecker.evict(userId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    blockedUserChecker.evict(userId);
                }
            });
        }
    }

    private User load(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User tidak ditemukan"));
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
