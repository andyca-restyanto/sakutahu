// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/AdminUserSummaryResponse.java
package com.example.app.modules.admin.dto;

import com.example.app.modules.subscription.entity.UserPlan;

import java.time.Instant;
import java.util.UUID;

/** plan = plan efektif (VIP yang sudah lewat masa berlakunya ditampilkan FREE). */
public record AdminUserSummaryResponse(
        UUID id,
        String fullName,
        String email,
        UserPlan plan,
        Instant planExpiresAt,
        boolean emailVerified,
        boolean telegramLinked,
        boolean blocked,
        Instant createdAt
) {}
