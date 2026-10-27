// filepath: /backend/src/main/java/com/example/app/modules/category/dto/CategoryResponse.java
package com.example.app.modules.category.dto;

import com.example.app.modules.category.entity.CategoryKind;
import com.example.app.modules.category.entity.TransactionType;

public record CategoryResponse(
        Long id,
        String code,
        String name,
        TransactionType type,
        CategoryKind kind,
        String color,
        String icon,
        int sortOrder
) {}
