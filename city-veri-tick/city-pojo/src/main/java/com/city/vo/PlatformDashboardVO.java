package com.city.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PlatformDashboardVO implements Serializable {
    private long totalActivities;
    private long pendingActivities;
    private long publishedActivities;
    private long totalOrganizers;
    private long approvedOrganizers;
    private long totalOrders;
    private long paidOrders;
    private BigDecimal paidRevenue = BigDecimal.ZERO;
    private long totalTickets;
    private long checkedInTickets;
    private BigDecimal verificationRate = BigDecimal.ZERO;
}
