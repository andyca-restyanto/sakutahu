// filepath: /backend/src/main/java/com/example/app/modules/dashboard/dto/DashboardPeriod.java
package com.example.app.modules.dashboard.dto;

import java.time.LocalDate;

public record DashboardPeriod(
        LocalDate from,
        LocalDate to,
        LocalDate previousFrom,
        LocalDate previousTo,
        Granularity granularity,
        String currency
) {}
