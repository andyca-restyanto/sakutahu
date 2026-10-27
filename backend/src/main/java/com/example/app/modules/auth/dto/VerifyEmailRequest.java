// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/VerifyEmailRequest.java
package com.example.app.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotBlank(message = "Token wajib diisi")
        @Size(max = 128, message = "Token tidak valid")
        String token
) {}
