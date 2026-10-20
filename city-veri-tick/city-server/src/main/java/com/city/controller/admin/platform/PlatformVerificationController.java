package com.city.controller.admin.platform;

import com.city.result.Result;
import com.city.service.PlatformOrderService;
import com.city.vo.OrganizerVerificationRecordVO;
import com.city.vo.OrganizerVerificationSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "平台核销监管接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/platform/verification")
public class PlatformVerificationController {
    @Autowired
    private PlatformOrderService platformOrderService;

    @Operation(summary = "查询平台核销汇总")
    @GetMapping("/summary")
    public Result<OrganizerVerificationSummaryVO> summary() {
        return Result.success(platformOrderService.verificationSummary());
    }

    @Operation(summary = "查询平台核销记录")
    @GetMapping("/records")
    public Result<List<OrganizerVerificationRecordVO>> records(@RequestParam(value = "checkedIn", required = false) Boolean checkedIn) {
        return Result.success(platformOrderService.verificationRecords(checkedIn));
    }
}
