// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/BlockUserRequest.java
package com.example.app.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockUserRequest(
        @NotBlank(message = "Alasan blokir wajib diisi")
        @Size(max = 255, message = "Alasan maksimal 255 karakter")
        String reason
) {}
