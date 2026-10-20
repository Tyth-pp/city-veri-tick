package com.city.controller.admin.platform;

import com.city.result.Result;
import com.city.service.PlatformOrderService;
import com.city.vo.OrderViewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "平台订单接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/platform/orders")
public class PlatformOrderController {
    @Autowired
    private PlatformOrderService platformOrderService;

    @Operation(summary = "查询平台订单")
    @GetMapping
    public Result<List<OrderViewVO>> list(@RequestParam(value = "status", required = false) String status) {
        return Result.success(platformOrderService.list(status));
    }

    @Operation(summary = "查询平台订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderViewVO> detail(@PathVariable("orderId") Long orderId) {
        return Result.success(platformOrderService.detail(orderId));
    }
}
