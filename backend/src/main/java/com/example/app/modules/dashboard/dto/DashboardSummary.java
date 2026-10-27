// filepath: /backend/src/main/java/com/example/app/modules/dashboard/dto/DashboardSummary.java
package com.example.app.modules.dashboard.dto;

import java.math.BigDecimal;

/**
 * spending = pengeluaran konsumtif (kategori kind SPENDING), saving = Tabungan & Investasi (kind SAVING),
 * balance = income - spending - saving, savingRate = saving / income × 100 (null jika income 0).
 */
public record DashboardSummary(
        BigDecimal income,
        BigDecimal spending,
        BigDecimal saving,
        BigDecimal balance,
        BigDecimal savingRate
) {}
