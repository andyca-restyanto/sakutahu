// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/ChangePasswordRequest.java
package com.example.app.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Password saat ini wajib diisi")
        @Size(max = 72, message = "Password terlalu panjang")
        String currentPassword,

        @NotBlank(message = "Password baru wajib diisi")
        @Size(min = 12, max = 72, message = "Password admin harus 12-72 karakter")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password harus mengandung huruf dan angka")
        String newPassword
) {}
