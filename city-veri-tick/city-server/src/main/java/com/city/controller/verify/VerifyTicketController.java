package com.city.controller.verify;

import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.dto.VerifyTicketDTO;
import com.city.service.CheckInService;
import com.city.vo.VerifyTicketViewVO;
import com.city.vo.VerifierStatisticsVO;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "验票接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/verify/tickets")
public class VerifyTicketController {

    @Autowired
    private CheckInService checkInService;

    @Operation(summary = "核销门票")
    @PostMapping("/check-in")
    /**
     * @param user 当前登录核销员
     * @param verifyTicketDTO 验票请求参数
     * @return 操作结果
     * @apiNote 核销用户门票
     */
    public Result<VerifyTicketViewVO> checkIn(@AuthenticationPrincipal LoginPrincipal user, @Valid @RequestBody VerifyTicketDTO verifyTicketDTO) {
        return Result.success(checkInService.checkIn(user.userId(), verifyTicketDTO.getVerificationCode(), verifyTicketDTO.getIdCardNo()));
    }

    @Operation(summary = "查询待核销门票")
    @GetMapping("/{verificationCode}")
    public Result<VerifyTicketViewVO> detail(@AuthenticationPrincipal LoginPrincipal user, @PathVariable("verificationCode") String verificationCode) {
        return Result.success(checkInService.detail(user.userId(), verificationCode));
    }

    @Operation(summary = "查询核销员最近记录")
    @GetMapping("/records")
    public Result<List<VerifyTicketViewVO>> records(@AuthenticationPrincipal LoginPrincipal user) {
        return Result.success(checkInService.records(user.userId()));
    }

    @Operation(summary = "查询核销员统计")
    @GetMapping("/statistics")
    public Result<VerifierStatisticsVO> statistics(@AuthenticationPrincipal LoginPrincipal user) {
        return Result.success(checkInService.statistics(user.userId()));
    }
}
