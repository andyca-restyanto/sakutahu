// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/ResetPasswordRequest.java
package com.example.app.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token wajib diisi")
        @Size(max = 100, message = "Token tidak valid")
        String token,

        @NotBlank(message = "Password baru wajib diisi")
        @Size(min = 8, max = 72, message = "Password harus 8-72 karakter")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password harus mengandung huruf dan angka")
        String newPassword
) {}
