package com.city.controller.admin.platform;
import com.city.service.ManagementDashboardService;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;

import com.city.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "平台数据看板接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/platform/dashboard")
public class PlatformDashboardController {
    @Autowired
    private ManagementDashboardService platformDashboardService;

    @Operation(summary = "查询平台运营概览")
    @GetMapping("/summary")
    public Result<Map<String,Object>> summary(@RequestParam(value="days",defaultValue="7") int days) {
        return Result.success(platformDashboardService.summary(null,days));
    }
}
