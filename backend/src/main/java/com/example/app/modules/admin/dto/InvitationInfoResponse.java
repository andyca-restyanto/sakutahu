// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/InvitationInfoResponse.java
package com.example.app.modules.admin.dto;

import java.time.Instant;

public record InvitationInfoResponse(
        String email,
        String fullName,
        Instant expiresAt
) {}
