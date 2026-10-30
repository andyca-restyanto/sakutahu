// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/UpdateAdminStatusRequest.java
package com.example.app.modules.admin.dto;

import com.example.app.modules.admin.entity.AdminStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAdminStatusRequest(
        @NotNull(message = "Status wajib diisi")
        AdminStatus status
) {}
