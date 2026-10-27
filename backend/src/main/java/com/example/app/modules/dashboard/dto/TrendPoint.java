// filepath: /backend/src/main/java/com/example/app/modules/dashboard/dto/TrendPoint.java
package com.example.app.modules.dashboard.dto;

import java.math.BigDecimal;

/** bucket: "yyyy-MM-dd" untuk DAY, "yyyy-MM" untuk MONTH. Bucket tanpa transaksi bernilai 0. */
public record TrendPoint(
        String bucket,
        BigDecimal income,
        BigDecimal spending,
        BigDecimal saving
) {}
