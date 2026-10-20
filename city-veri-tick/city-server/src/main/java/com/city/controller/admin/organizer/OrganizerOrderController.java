package com.city.controller.admin.organizer;

import com.city.dto.RefundReviewDTO;
import com.city.entity.RefundRequest;
import com.city.service.RefundService;
import jakarta.validation.Valid;

import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.OrganizerOrderService;
import com.city.vo.OrderViewVO;
import com.city.vo.OrganizerVerificationRecordVO;
import com.city.vo.OrganizerVerificationSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "主办方订单与核销接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/organizer")
public class OrganizerOrderController {
    @Autowired
    private OrganizerOrderService organizerOrderService;
    @Autowired
    private RefundService refundService;

    @Operation(summary = "审核整单模拟退款")
    @PostMapping("/orders/{orderId}/refunds/{refundId}/review")
    public Result<RefundRequest> review(@AuthenticationPrincipal LoginPrincipal user,
                                        @PathVariable("orderId") Long orderId, @PathVariable("refundId") Long refundId,
                                        @Valid @RequestBody RefundReviewDTO dto) {
        return Result.success(refundService.review(user.userId(), orderId, refundId, dto.getApproved(), dto.getReason()));
    }

    @Operation(summary = "查询主办方活动订单")
    @GetMapping("/orders")
    public Result<List<OrderViewVO>> list(@AuthenticationPrincipal LoginPrincipal user,
                                          @RequestParam(value = "status", required = false) String status) {
        return Result.success(organizerOrderService.list(user.userId(), status));
    }

    @Operation(summary = "查询主办方订单详情")
    @GetMapping("/orders/{orderId}")
    public Result<OrderViewVO> detail(@AuthenticationPrincipal LoginPrincipal user,
                                      @PathVariable("orderId") Long orderId) {
        return Result.success(organizerOrderService.detail(user.userId(), orderId));
    }

    @Operation(summary = "查询主办方核销汇总")
    @GetMapping("/verification/summary")
    public Result<OrganizerVerificationSummaryVO> summary(@AuthenticationPrincipal LoginPrincipal user) {
        return Result.success(organizerOrderService.verificationSummary(user.userId()));
    }

    @Operation(summary = "查询主办方核销记录")
    @GetMapping("/verification/records")
    public Result<List<OrganizerVerificationRecordVO>> records(@AuthenticationPrincipal LoginPrincipal user,
                                                               @RequestParam(value = "checkedIn", required = false) Boolean checkedIn) {
        return Result.success(organizerOrderService.verificationRecords(user.userId(), checkedIn));
    }
}
