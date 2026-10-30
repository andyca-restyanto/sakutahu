// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/ReceiptScanSettingsResponse.java
package com.example.app.modules.admin.dto;

import com.example.app.modules.subscription.entity.FeatureAccessMode;

import java.time.Instant;

public record ReceiptScanSettingsResponse(
        FeatureAccessMode access,
        int freeMonthlyQuota,
        Instant updatedAt,
        ReceiptScanUsage usage
) {}
