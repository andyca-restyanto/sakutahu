// filepath: /backend/src/main/java/com/example/app/modules/dashboard/dto/DashboardComparison.java
package com.example.app.modules.dashboard.dto;

import java.math.BigDecimal;

/** Persentase perubahan terhadap periode sebelumnya. null jika periode sebelumnya bernilai 0. */
public record DashboardComparison(
        BigDecimal income,
        BigDecimal spending,
        BigDecimal saving
) {}
