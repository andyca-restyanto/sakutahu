// filepath: /backend/src/main/java/com/example/app/modules/category/dto/CategorySummaryResponse.java
package com.example.app.modules.category.dto;

import com.example.app.modules.category.entity.CategoryKind;

/** Ringkasan kategori yang disematkan di response transaksi. */
public record CategorySummaryResponse(
        Long id,
        String code,
        String name,
        CategoryKind kind,
        String color,
        String icon
) {}
