// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/ResetPasswordInfoResponse.java
package com.example.app.modules.auth.dto;

import java.time.Instant;

/** Email ditampilkan tersamar (mis. bu***@mail.com). */
public record ResetPasswordInfoResponse(
        String maskedEmail,
        Instant expiresAt
) {}
