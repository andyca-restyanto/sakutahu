// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/ResendVerificationRequest.java
package com.example.app.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @NotBlank(message = "Email wajib diisi")
        @Email(message = "Format email tidak valid")
        String email
) {}
