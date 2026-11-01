// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/AdminResetPasswordRequest.java
package com.example.app.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
        @NotBlank(message = "Token wajib diisi")
        @Size(max = 100, message = "Token tidak valid")
        String token,

        @NotBlank(message = "Password baru wajib diisi")
        @Size(min = 12, max = 72, message = "Password admin harus 12-72 karakter")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password harus mengandung huruf dan angka")
        String newPassword
) {}
