package com.city.controller.user;

import com.city.vo.OrderPaymentPolicyVO;
import com.city.dto.RefundApplyDTO;
import com.city.entity.RefundRequest;
import com.city.service.RefundService;

import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.dto.CreateOrderDTO;
import com.city.vo.OrderViewVO;
import com.city.service.OrderService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户订单接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/user/orders")
public class UserOrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private RefundService refundService;

    @Operation(summary = "申请整单模拟退款")
    @PostMapping("/{orderId}/refunds")
    public Result<RefundRequest> refund(@AuthenticationPrincipal LoginPrincipal user,
                                        @PathVariable("orderId") Long orderId, @Valid @RequestBody RefundApplyDTO dto) {
        return Result.success(refundService.apply(user.userId(), orderId, dto.getReason()));
    }

    @Operation(summary = "创建订单")
    @PostMapping
    /**
     * @param user 当前登录用户
     * @param createOrderDTO 创建订单请求参数
     * @return 创建的门票列表
     * @apiNote 用户创建订单
     */
    public Result<OrderViewVO> create(@AuthenticationPrincipal LoginPrincipal user,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                        @Valid @RequestBody CreateOrderDTO createOrderDTO) {
        return Result.success(orderService.create(user.userId(), createOrderDTO, idempotencyKey));
    }

    @Operation(summary = "查询订单支付时限", description = "返回配置的支付时限（秒）和服务器时间；创建订单前的页面据此说明支付期限，不会创建订单。")
    @GetMapping("/payment-policy")
    public Result<OrderPaymentPolicyVO> paymentPolicy() {
        return Result.success(orderService.paymentPolicy());
    }

    @Operation(summary = "同步本人订单超时状态", description = "仅处理当前账号的指定订单。待支付订单达到截止时间时按事务关闭并释放锁定库存及优惠券；重复调用不重复释放，返回最新订单状态。")
    @PostMapping("/{orderId}/sync-expiry")
    public Result<OrderViewVO> synchronizeExpiry(@Parameter(hidden = true) @AuthenticationPrincipal LoginPrincipal user,
                                                @Parameter(description = "本人订单ID", required = true, example = "1") @PathVariable("orderId") Long orderId) {
        return Result.success(orderService.synchronizeExpiry(user.userId(), orderId));
    }

    @Operation(summary = "模拟支付订单")
    @PostMapping("/{orderId}/pay")
    public Result<OrderViewVO> pay(@AuthenticationPrincipal LoginPrincipal user,
                                   @PathVariable("orderId") Long orderId) {
        return Result.success(orderService.pay(user.userId(), orderId));
    }

    @Operation(summary = "取消待支付订单")
    @PostMapping("/{orderId}/cancel")
    public Result<OrderViewVO> cancel(@AuthenticationPrincipal LoginPrincipal user,
                                      @PathVariable("orderId") Long orderId) {
        return Result.success(orderService.cancel(user.userId(), orderId));
    }

    @Operation(summary = "查询本人订单列表")
    @GetMapping
    /**
     * @param user 当前登录用户
     * @param status 订单状态，可选
     * @return 本人的订单列表
     * @apiNote 查询当前登录用户的订单列表
     */
    public Result<List<OrderViewVO>> list(@AuthenticationPrincipal LoginPrincipal user,
                                          @RequestParam(value = "status", required = false) String status) {
        return Result.success(orderService.list(user.userId(), status));
    }

    @Operation(summary = "查询本人订单详情")
    @GetMapping("/{orderId}")
    /**
     * @param user 当前登录用户
     * @param orderId 订单ID
     * @return 本人的订单详情
     * @apiNote 查询当前登录用户的订单详情
     */
    public Result<OrderViewVO> detail(@AuthenticationPrincipal LoginPrincipal user,
                                      @PathVariable("orderId") Long orderId) {
        return Result.success(orderService.detail(user.userId(), orderId));
    }
}
