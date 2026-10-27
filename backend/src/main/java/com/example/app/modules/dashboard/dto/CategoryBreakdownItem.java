// filepath: /backend/src/main/java/com/example/app/modules/dashboard/dto/CategoryBreakdownItem.java
package com.example.app.modules.dashboard.dto;

import java.math.BigDecimal;

public record CategoryBreakdownItem(
        Long categoryId,
        String code,
        String name,
        String color,
        String icon,
        BigDecimal amount,
        BigDecimal percentage,
        long count
) {}
