// filepath: /backend/src/main/java/com/example/app/modules/dashboard/dto/DashboardByCategory.java
package com.example.app.modules.dashboard.dto;

import java.util.List;

/**
 * spending: hanya kategori kind SPENDING (Tabungan & Investasi dipisah, lihat summary.saving).
 * income: semua kategori pemasukan. Persentase dihitung terhadap total masing-masing daftar.
 */
public record DashboardByCategory(
        List<CategoryBreakdownItem> spending,
        List<CategoryBreakdownItem> income
) {}
