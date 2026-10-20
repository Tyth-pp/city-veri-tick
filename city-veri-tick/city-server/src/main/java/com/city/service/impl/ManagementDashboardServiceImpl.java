package com.city.service.impl;

import com.city.constant.ManagementMessageConstant;
import com.city.service.ManagementDashboardService;

import com.city.mapper.ManagementDashboardMapper;
import org.springframework.stereotype.Service;

import java.time.*;
import java.math.*;
import java.util.*;

@Service
public class ManagementDashboardServiceImpl implements ManagementDashboardService {
    private final ManagementDashboardMapper mapper;
    private final Clock clock;

    public ManagementDashboardServiceImpl(ManagementDashboardMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public Map<String, Object> summary(Long organizerId, int days) {
        if (days != 7 && days != 30) throw new IllegalArgumentException(ManagementMessageConstant.DASHBOARD_RANGE_INVALID);
        LocalDate today = LocalDate.now(clock);
        LocalDateTime from = today.minusDays(days - 1).atStartOfDay(), to = today.plusDays(1).atStartOfDay();
        Map<String, Object> result = new LinkedHashMap<>();
        merge(result, mapper.activityMetrics(organizerId));
        merge(result, mapper.paymentMetrics(organizerId, from, to));
        merge(result, mapper.refundMetrics(organizerId, from, to));
        merge(result, mapper.ticketMetrics(organizerId));
        result.put("netAmount", number(result.get("paidRevenue")).subtract(number(result.get("refundAmount"))));
        BigDecimal valid = number(result.get("validTickets"));
        result.put("verificationRate", valid.signum() == 0 ? BigDecimal.ZERO : number(result.get("checkedInTickets")).multiply(BigDecimal.valueOf(100)).divide(valid, 2, RoundingMode.HALF_UP));
        result.put("todayPayment", mapper.paymentMetrics(organizerId, today.atStartOfDay(), to));
        Map<String, Map<String, Object>> existing = new HashMap<>();
        List<Map<String, Object>> rows = mapper.trend(organizerId, from, to);
        if (rows != null) for (var row : rows) existing.put(String.valueOf(row.get("day")), row);
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String day = today.minusDays(days - 1 - i).toString();
            trend.add(existing.getOrDefault(day, Map.of("day", day, "payment", 0, "refund", 0, "issued", 0, "checked", 0)));
        }
        result.put("trend", trend);
        result.put("categories", mapper.categories(organizerId));
        result.put("tasks", mapper.tasks(organizerId));
        result.put("todaySessions", mapper.sessions(organizerId, today.atStartOfDay(), to));
        result.put("ranking", mapper.ranking(organizerId, from, to));
        List<Map<String, Object>> recent = new ArrayList<>();
        if (organizerId == null) {
            recent.addAll(mapper.recentAccountOperations());
            recent.addAll(mapper.recentActivityReviews());
        }
        result.put("recentOperations", recent.stream().sorted(Comparator.comparing((Map<String, Object> r) -> String.valueOf(r.get("time"))).reversed()).limit(8).toList());
        result.put("fromDate", from.toLocalDate().toString());
        result.put("toDate", today.toString());
        result.put("days", days);
        result.put("generatedAt", LocalDateTime.now(clock).toString());
        result.put("paymentMode", "SIMULATED");
        return result;
    }

    private static void merge(Map<String, Object> target, Map<String, Object> values) {
        if (values != null) target.putAll(values);
    }

    private static BigDecimal number(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
