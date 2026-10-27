// filepath: /backend/src/main/java/com/example/app/modules/subscription/dto/FeatureAccessResponse.java
package com.example.app.modules.subscription.dto;

import com.example.app.modules.subscription.entity.UserPlan;

import java.time.Instant;

public record FeatureAccessResponse(
        UserPlan plan,
        Instant planExpiresAt,
        ReceiptScanAccess receiptScan
) {}
