// filepath: /backend/src/main/java/com/example/app/modules/subscription/dto/ReceiptScanAccess.java
package com.example.app.modules.subscription.dto;

import com.example.app.modules.subscription.entity.FeatureAccessMode;
import com.example.app.modules.subscription.entity.FeatureDenyReason;

/** monthlyQuota null = tanpa batas (VIP). */
public record ReceiptScanAccess(
        boolean available,
        FeatureAccessMode access,
        FeatureDenyReason reason,
        Integer monthlyQuota,
        int usedThisMonth
) {}
