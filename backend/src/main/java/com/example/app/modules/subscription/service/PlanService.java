// filepath: /backend/src/main/java/com/example/app/modules/subscription/service/PlanService.java
package com.example.app.modules.subscription.service;

import com.example.app.modules.auth.entity.User;
import com.example.app.modules.subscription.entity.UserPlan;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class PlanService {

    private final Clock clock;

    public PlanService(Clock clock) {
        this.clock = clock;
    }

    /** VIP yang masa berlakunya sudah lewat dianggap FREE. */
    public UserPlan effectivePlan(User user) {
        if (user.getPlan() == UserPlan.VIP
                && user.getPlanExpiresAt() != null
                && !user.getPlanExpiresAt().isAfter(clock.instant())) {
            return UserPlan.FREE;
        }
        return user.getPlan();
    }
}
