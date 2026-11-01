// filepath: /backend/src/main/java/com/example/app/modules/report/dto/CategoryRecapResponse.java
package com.example.app.modules.report.dto;

import com.example.app.modules.category.entity.CategoryKind;

import java.math.BigDecimal;

/** Rekap per kategori untuk sheet "Ringkasan" di Excel. share = porsi dari total kind yang sama (0–1). */
public record CategoryRecapResponse(
        Long categoryId,
        String name,
        String icon,
        CategoryKind kind,
        long count,
        BigDecimal total,
        double share
) {}
