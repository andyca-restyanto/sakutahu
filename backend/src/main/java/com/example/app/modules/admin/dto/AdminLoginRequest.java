// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/AdminLoginRequest.java
package com.example.app.modules.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank(message = "Email wajib diisi")
        @Email(message = "Format email tidak valid")
        String email,

        @NotBlank(message = "Password wajib diisi")
        @Size(max = 72, message = "Password terlalu panjang")
        String password
) {}
