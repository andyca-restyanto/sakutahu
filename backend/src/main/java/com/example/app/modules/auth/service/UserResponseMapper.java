// filepath: /backend/src/main/java/com/example/app/modules/auth/service/UserResponseMapper.java
package com.example.app.modules.auth.service;

import com.example.app.modules.auth.dto.UserResponse;
import com.example.app.modules.auth.entity.User;
import com.example.app.modules.subscription.service.PlanService;
import com.example.app.modules.telegram.service.TelegramAccountService;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {

    private final TelegramAccountService telegramAccountService;
    private final PlanService planService;

    public UserResponseMapper(TelegramAccountService telegramAccountService, PlanService planService) {
        this.telegramAccountService = telegramAccountService;
        this.planService = planService;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getCurrency(),
                user.getTimezone(),
                telegramAccountService.isLinked(user.getId()),
                planService.effectivePlan(user),
                user.getCreatedAt()
        );
    }
}
