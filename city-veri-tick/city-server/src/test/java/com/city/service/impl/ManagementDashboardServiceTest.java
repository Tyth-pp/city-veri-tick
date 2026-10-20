package com.city.service.impl;

import com.city.mapper.ManagementDashboardMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManagementDashboardServiceTest {
    @Test void fillsMissingDaysWithZerosAndKeepsOrganizationScope() {
        var mapper=mock(ManagementDashboardMapper.class);
        var service=new ManagementDashboardServiceImpl(mapper,Clock.fixed(Instant.parse("2026-10-16T04:00:00Z"),ZoneId.of("Asia/Shanghai")));
        when(mapper.activityMetrics(9L)).thenReturn(new HashMap<>(Map.of("totalActivities",3L)));
        when(mapper.paymentMetrics(eq(9L),any(),any())).thenReturn(Map.of("paidRevenue",new java.math.BigDecimal("100")));
        when(mapper.refundMetrics(eq(9L),any(),any())).thenReturn(Map.of("refundAmount",new java.math.BigDecimal("25")));
        var result=service.summary(9L,7);
        assertThat(result.get("netAmount")).isEqualTo(new java.math.BigDecimal("75"));
        assertThat((List<?>)result.get("trend")).hasSize(7);
        verify(mapper).ticketMetrics(9L);
        verify(mapper,never()).activityMetrics(null);
    }
    @Test void limitsPeriodsToSupportedValues() {
        var service=new ManagementDashboardServiceImpl(mock(ManagementDashboardMapper.class),Clock.systemUTC());
        assertThatThrownBy(()->service.summary(null,365)).isInstanceOf(IllegalArgumentException.class);
    }
}
