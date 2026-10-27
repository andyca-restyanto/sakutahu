// filepath: /backend/src/main/java/com/example/app/modules/dashboard/dto/DashboardResponse.java
package com.example.app.modules.dashboard.dto;

import java.util.List;

public record DashboardResponse(
        DashboardPeriod period,
        DashboardSummary summary,
        DashboardComparison comparison,
        DashboardByCategory byCategory,
        List<TrendPoint> trend
) {}
