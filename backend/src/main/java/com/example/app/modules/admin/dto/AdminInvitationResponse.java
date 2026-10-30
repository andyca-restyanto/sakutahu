// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/AdminInvitationResponse.java
package com.example.app.modules.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminInvitationResponse(
        UUID id,
        String email,
        String fullName,
        String invitedByName,
        Instant expiresAt,
        boolean expired,
        Instant createdAt
) {}
