// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/AdminResetPasswordInfoResponse.java
package com.example.app.modules.admin.dto;

import java.time.Instant;

/** Email ditampilkan tersamar (mis. an***@gmail.com). */
public record AdminResetPasswordInfoResponse(
        String maskedEmail,
        Instant expiresAt
) {}
