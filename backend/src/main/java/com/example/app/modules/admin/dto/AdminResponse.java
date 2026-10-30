// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/AdminResponse.java
package com.example.app.modules.admin.dto;

import com.example.app.modules.admin.entity.AdminRole;
import com.example.app.modules.admin.entity.AdminStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminResponse(
        UUID id,
        String fullName,
        String email,
        AdminRole role,
        AdminStatus status,
        boolean mustChangePassword,
        Instant lastLoginAt,
        Instant createdAt
) {}
