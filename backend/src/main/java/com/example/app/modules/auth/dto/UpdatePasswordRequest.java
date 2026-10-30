// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/UpdatePasswordRequest.java
package com.example.app.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Ganti password oleh user yang sedang login. */
public record UpdatePasswordRequest(
        @NotBlank(message = "Password saat ini wajib diisi")
        @Size(max = 72, message = "Password terlalu panjang")
        String currentPassword,

        @NotBlank(message = "Password baru wajib diisi")
        @Size(min = 8, max = 72, message = "Password harus 8-72 karakter")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password harus mengandung huruf dan angka")
        String newPassword
) {}
