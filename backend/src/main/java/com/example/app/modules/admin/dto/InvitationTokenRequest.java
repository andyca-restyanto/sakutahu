// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/InvitationTokenRequest.java
package com.example.app.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitationTokenRequest(
        @NotBlank(message = "Token wajib diisi")
        @Size(max = 100, message = "Token tidak valid")
        String token
) {}
