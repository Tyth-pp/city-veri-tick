package com.city.service;

import java.util.Map;

/** 管理端经营与核销统计的业务契约。 */
public interface ManagementDashboardService {
    Map<String, Object> summary(Long organizerId, int days);
}
