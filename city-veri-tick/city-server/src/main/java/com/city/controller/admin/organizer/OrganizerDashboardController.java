package com.city.controller.admin.organizer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.OrganizerAccessService;
import com.city.service.ManagementDashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 组织者仪表板控制器
 * 提供组织者数据统计和概览相关的API接口
 */
@Tag(name = "主办方工作台接口")
@RestController
@RequestMapping("/controller/admin/organizer/dashboard")
public class OrganizerDashboardController {
    // 仪表板服务，用于获取统计数据
    private final ManagementDashboardService dashboard;
    // 组织者访问服务，用于处理组织者相关的访问权限
    private final OrganizerAccessService access;

    /**
     * 构造函数，通过依赖注入初始化服务
     * @param dashboard 仪表板服务
     * @param access 组织者访问服务
     */
    public OrganizerDashboardController(ManagementDashboardService dashboard, OrganizerAccessService access) {
        this.dashboard = dashboard;
        this.access = access;
    }

    /**
     * 获取组织者仪表板数据概览
     * @param user 当前登录用户信息
     * @param days 统计天数，默认为7天
     * @return 返回包含统计数据的结果对象
     */
    @Operation(summary = "查询主办方工作台概览")
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@AuthenticationPrincipal LoginPrincipal user, @RequestParam(value = "days", defaultValue = "7") int days) {
        return Result.success(dashboard.summary(access.requireOrganizerId(user.userId()), days));
    }
}
