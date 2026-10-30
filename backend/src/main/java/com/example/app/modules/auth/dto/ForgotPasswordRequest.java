// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/ForgotPasswordRequest.java
package com.example.app.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email wajib diisi")
        @Email(message = "Format email tidak valid")
        @Size(max = 254, message = "Email terlalu panjang")
        String email
) {}
