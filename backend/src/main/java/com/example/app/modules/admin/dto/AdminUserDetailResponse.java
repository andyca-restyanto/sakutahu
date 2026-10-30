// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/AdminUserDetailResponse.java
package com.example.app.modules.admin.dto;

import com.example.app.modules.subscription.entity.UserPlan;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Data akun & statistik saja; isi transaksi user sengaja tidak ditampilkan ke admin. */
public record AdminUserDetailResponse(
        UUID id,
        String fullName,
        String email,
        UserPlan plan,
        Instant planExpiresAt,
        boolean emailVerified,
        boolean telegramLinked,
        String telegramUsername,
        String timezone,
        long transactionCount,
        int receiptScansThisMonth,
        boolean blocked,
        Instant blockedAt,
        String blockedReason,
        Instant createdAt,
        List<AdminAuditEntryResponse> history
) {}
