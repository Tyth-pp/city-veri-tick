package com.city.controller.admin.platform;

import com.city.dto.AssignVerifierDTO;
import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.PlatformVerifierService;
import com.city.vo.PlatformVerifierViewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "平台核销员授权接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/platform/verifiers")
public class PlatformVerifierController {
    @Autowired
    private PlatformVerifierService platformVerifierService;

    @Operation(summary = "查询核销员授权")
    @GetMapping
    public Result<List<PlatformVerifierViewVO>> list(@RequestParam(value = "activityId", required = false) Long activityId,
                                                     @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return Result.success(platformVerifierService.list(activityId, enabled));
    }

    @Operation(summary = "授权核销员")
    @PostMapping
    public Result<Void> assign(@AuthenticationPrincipal LoginPrincipal user, @Valid @RequestBody AssignVerifierDTO dto) {
        platformVerifierService.assign(user.userId(), dto);
        return Result.success();
    }

    @Operation(summary = "更新核销员授权状态")
    @PatchMapping("/{id}")
    public Result<Void> updateEnabled(@PathVariable("id") Long id, @RequestParam("enabled") boolean enabled) {
        platformVerifierService.updateEnabled(id, enabled);
        return Result.success();
    }
}
