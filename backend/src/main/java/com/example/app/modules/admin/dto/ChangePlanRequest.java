// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/ChangePlanRequest.java
package com.example.app.modules.admin.dto;

import com.example.app.modules.subscription.entity.UserPlan;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** planExpiresAt: null = VIP tanpa batas waktu; diabaikan untuk FREE. */
public record ChangePlanRequest(
        @NotNull(message = "Plan wajib diisi")
        UserPlan plan,

        Instant planExpiresAt
) {}
