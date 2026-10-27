// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/UserResponse.java
package com.example.app.modules.auth.dto;

import com.example.app.modules.subscription.entity.UserPlan;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        boolean emailVerified,
        String currency,
        String timezone,
        boolean telegramLinked,
        UserPlan plan,
        Instant createdAt
) {}
