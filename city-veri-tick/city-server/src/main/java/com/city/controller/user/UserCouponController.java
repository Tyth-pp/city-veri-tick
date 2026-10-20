package com.city.controller.user;

import com.city.entity.Coupon;
import com.city.vo.CouponChoiceVO;
import com.city.vo.CouponWalletVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.city.entity.UserCoupon;
import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户优惠券接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/user/coupons")
public class UserCouponController {
    @Autowired
    private CouponService coupons;

    @Operation(summary = "领取优惠券", description = "领取普通、限量或秒杀优惠券；限量与秒杀先由Redis原子预占，数据库提交成功后返回个人券。个人不限次数仍受总库存和领取窗口约束。")
    @PostMapping("/{couponId}/claim")
    public Result<UserCoupon> claim(@Parameter(hidden = true) @AuthenticationPrincipal LoginPrincipal user, @Parameter(description = "优惠券定义ID", required = true, example = "1") @PathVariable("couponId") Long couponId) {
        return Result.success(coupons.claim(user.userId(), couponId));
    }

    @Operation(summary = "查询领券中心列表", description = "返回可领取普通券及未结束的限量、秒杀券，包含未开抢、已抢光和个人限领状态。")
    @GetMapping("/available")
    public Result<List<Coupon>> available(@Parameter(hidden = true) @AuthenticationPrincipal LoginPrincipal user) {
        return Result.success(coupons.available(user.userId()));
    }

    @Operation(summary = "查询我的优惠券及状态统计", description = "仅返回当前账号的券包、优惠规则和订单关联，区分未使用、已使用及已失效状态。")
    @GetMapping("/mine")
    public Result<CouponWalletVO> mine(@Parameter(hidden = true) @AuthenticationPrincipal LoginPrincipal user) {
        return Result.success(coupons.mine(user.userId()));
    }

    @Operation(summary = "试算当前订单可用优惠券", description = "根据真实票价、购票数量、活动及分类计算优惠，返回可用性、不可用原因和抵扣金额；正式下单时再次校验。")
    @GetMapping("/choices")
    public Result<List<CouponChoiceVO>> choices(@Parameter(hidden = true) @AuthenticationPrincipal LoginPrincipal user,
                                                            @Parameter(description = "购票票种ID", required = true, example = "1") @RequestParam("ticketTypeId") Long ticketTypeId, @Parameter(description = "购票数量，1至100张", required = true, example = "1") @RequestParam("quantity") int quantity) {
        return Result.success(coupons.choices(user.userId(), ticketTypeId, quantity));
    }

    @Operation(summary = "查询本人领券原始记录", description = "返回当前账号的个人券原始记录；查看完整规则和分类统计请使用我的优惠券接口。")
    @GetMapping
    public Result<List<UserCoupon>> list(@Parameter(hidden = true) @AuthenticationPrincipal LoginPrincipal user) {
        return Result.success(coupons.list(user.userId()));
    }
}

