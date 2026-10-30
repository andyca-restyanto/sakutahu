// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/ResetPasswordInspectRequest.java
package com.example.app.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordInspectRequest(
        @NotBlank(message = "Token wajib diisi")
        @Size(max = 100, message = "Token tidak valid")
        String token
) {}
