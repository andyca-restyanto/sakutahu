// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/InviteAdminRequest.java
package com.example.app.modules.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteAdminRequest(
        @NotBlank(message = "Email wajib diisi")
        @Email(message = "Format email tidak valid")
        @Size(max = 254, message = "Email terlalu panjang")
        String email,

        @NotBlank(message = "Nama wajib diisi")
        @Size(max = 100, message = "Nama maksimal 100 karakter")
        String fullName
) {}
