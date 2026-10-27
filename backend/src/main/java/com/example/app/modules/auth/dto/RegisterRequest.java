// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/RegisterRequest.java
package com.example.app.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Nama wajib diisi")
        @Size(max = 100, message = "Nama maksimal 100 karakter")
        String fullName,

        @NotBlank(message = "Email wajib diisi")
        @Email(message = "Format email tidak valid")
        @Size(max = 254, message = "Email terlalu panjang")
        String email,

        @NotBlank(message = "Password wajib diisi")
        @Size(min = 8, max = 72, message = "Password harus 8-72 karakter")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password harus mengandung huruf dan angka")
        String password,

        /* Opsional, default IDR */
        @Pattern(regexp = "^[A-Z]{3}$", message = "Kode mata uang harus 3 huruf kapital")
        String currency,

        /* Opsional, default Asia/Jakarta */
        @Size(max = 64, message = "Zona waktu tidak valid")
        String timezone
) {}
