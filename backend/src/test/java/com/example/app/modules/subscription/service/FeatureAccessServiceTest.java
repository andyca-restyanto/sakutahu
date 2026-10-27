// filepath: /backend/src/test/java/com/example/app/modules/subscription/service/FeatureAccessServiceTest.java
package com.example.app.modules.subscription.service;

import com.example.app.modules.subscription.dto.ReceiptScanAccess;
import com.example.app.modules.subscription.entity.FeatureAccessMode;
import com.example.app.modules.subscription.entity.FeatureDenyReason;
import com.example.app.modules.subscription.entity.UserPlan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureAccessServiceTest {

    @Test
    void vipAlwaysAllowedWithoutQuota() {
        for (FeatureAccessMode mode : FeatureAccessMode.values()) {
            ReceiptScanAccess a = FeatureAccessService.decide(UserPlan.VIP, mode, 3, 50);
            assertThat(a.available()).isTrue();
            assertThat(a.monthlyQuota()).isNull();
            assertThat(a.reason()).isNull();
        }
    }

    @Test
    void freeBlockedWhenVipOnly() {
        ReceiptScanAccess a = FeatureAccessService.decide(UserPlan.FREE, FeatureAccessMode.VIP_ONLY, 3, 0);
        assertThat(a.available()).isFalse();
        assertThat(a.reason()).isEqualTo(FeatureDenyReason.VIP_ONLY);
    }

    @Test
    void freeHasQuotaWhenAll() {
        assertThat(FeatureAccessService.decide(UserPlan.FREE, FeatureAccessMode.ALL, 3, 2).available()).isTrue();
        ReceiptScanAccess full = FeatureAccessService.decide(UserPlan.FREE, FeatureAccessMode.ALL, 3, 3);
        assertThat(full.available()).isFalse();
        assertThat(full.reason()).isEqualTo(FeatureDenyReason.QUOTA_EXCEEDED);
        assertThat(full.monthlyQuota()).isEqualTo(3);
        assertThat(full.usedThisMonth()).isEqualTo(3);
    }
}
