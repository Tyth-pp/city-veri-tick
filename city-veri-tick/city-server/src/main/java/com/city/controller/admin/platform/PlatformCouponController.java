package com.city.controller.admin.platform;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.city.dto.CreateCouponDTO;
import com.city.entity.Coupon;
import com.city.result.Result;
import com.city.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "平台优惠券管理接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/platform/coupons")
public class PlatformCouponController {
    @Autowired
    CouponService coupons;

    @Operation(summary = "查询优惠券投放列表", description = "平台管理员查询全部优惠券定义、适用范围、发放数量与投放状态。")
    @GetMapping
    public Result<List<Coupon>> list() {
        return Result.success(coupons.adminList());
    }

    @Operation(summary = "创建未启用优惠券", description = "保存后状态为DRAFT（未启用），用户端不展示且不能领取；需管理员手动启用后才按领取时间开放。支持满减、折扣、无门槛优惠及普通、限量、秒杀发放。优惠封顶为空表示不封顶，每人限领为0表示个人不限次数；总量、时间及最低实付规则继续生效。")
    @PostMapping
    public Result<Coupon> create(@Valid @RequestBody CreateCouponDTO dto) {
        return Result.success(coupons.create(dto));
    }

    @Operation(summary = "启用或停用优惠券投放", description = "value仅支持ACTIVE或INACTIVE。修改提交后失效Redis领券快照；个人券使用状态继续按订单及券规则校验。")
    @PatchMapping("/{id}/status")
    public Result<Void> status(@Parameter(description = "优惠券定义ID", required = true, example = "1") @PathVariable("id") Long id, @Parameter(description = "投放状态", required = true, schema = @Schema(allowableValues = {"ACTIVE", "INACTIVE"})) @RequestParam("value") String value) {
        coupons.setStatus(id, value);
        return Result.success();
    }
}

