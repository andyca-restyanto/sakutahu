// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/UpdateReceiptScanSettingsRequest.java
package com.example.app.modules.admin.dto;

import com.example.app.modules.subscription.entity.FeatureAccessMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateReceiptScanSettingsRequest(
        @NotNull(message = "Akses wajib diisi")
        FeatureAccessMode access,

        @NotNull(message = "Kuota wajib diisi")
        @Min(value = 0, message = "Kuota minimal 0")
        @Max(value = 100, message = "Kuota maksimal 100")
        Integer freeMonthlyQuota
) {}
