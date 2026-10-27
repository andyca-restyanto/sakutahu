// filepath: /backend/src/test/java/com/example/app/modules/dashboard/service/DashboardServiceTest.java
package com.example.app.modules.dashboard.service;

import com.example.app.modules.dashboard.dto.Granularity;
import com.example.app.shared.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardServiceTest {

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    @Test
    void fullMonthComparesWithPreviousFullMonth() {
        assertThat(DashboardService.previousPeriod(d("2026-10-01"), d("2026-10-31")))
                .containsExactly(d("2026-09-01"), d("2026-09-30"));
        assertThat(DashboardService.previousPeriod(d("2026-03-01"), d("2026-03-31")))
                .containsExactly(d("2026-02-01"), d("2026-02-28"));
        // 3 bulan penuh
        assertThat(DashboardService.previousPeriod(d("2026-08-01"), d("2026-10-31")))
                .containsExactly(d("2026-05-01"), d("2026-07-31"));
    }

    @Test
    void customRangeComparesWithSameNumberOfDays() {
        assertThat(DashboardService.previousPeriod(d("2026-10-05"), d("2026-10-11")))
                .containsExactly(d("2026-09-28"), d("2026-10-04"));
    }

    @Test
    void granularityDependsOnLength() {
        assertThat(DashboardService.granularityOf(d("2026-10-01"), d("2026-10-31"))).isEqualTo(Granularity.DAY);
        assertThat(DashboardService.granularityOf(d("2026-10-01"), d("2026-11-01"))).isEqualTo(Granularity.MONTH);
    }

    @Test
    void changeAndPercent() {
        assertThat(DashboardService.change(new BigDecimal("150"), new BigDecimal("100"))).isEqualByComparingTo("50.0");
        assertThat(DashboardService.change(new BigDecimal("50"), new BigDecimal("200"))).isEqualByComparingTo("-75.0");
        assertThat(DashboardService.change(new BigDecimal("50"), BigDecimal.ZERO)).isNull();
        assertThat(DashboardService.percentOf(new BigDecimal("1"), new BigDecimal("3"))).isEqualByComparingTo("33.3");
        assertThat(DashboardService.percentOf(BigDecimal.ONE, BigDecimal.ZERO)).isNull();
    }

    @Test
    void rangeValidation() {
        assertThatCode(() -> DashboardService.validateRange(d("2024-11-01"), d("2026-10-31"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> DashboardService.validateRange(d("2024-10-31"), d("2026-10-31")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> DashboardService.validateRange(d("2026-10-02"), d("2026-10-01")))
                .isInstanceOf(ApiException.class);
    }
}
