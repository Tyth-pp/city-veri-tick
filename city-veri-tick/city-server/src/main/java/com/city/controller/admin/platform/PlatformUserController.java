package com.city.controller.admin.platform;

import com.city.dto.AccountOperationDTO;
import com.city.dto.StaffProfileDTO;
import com.city.entity.AccountOperation;
import com.city.service.AccountOperationService;
import com.city.service.StaffProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.city.result.Result;
import com.city.service.PlatformUserService;
import com.city.vo.PlatformUserViewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import com.city.security.LoginPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;

@Tag(name = "平台用户接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/platform/users")
public class PlatformUserController {
    @Autowired
    private PlatformUserService platformUserService;
    @Autowired
    private StaffProfileService profiles;
    @Autowired
    private AccountOperationService operations;

    @Operation(summary = "查询账号管理操作记录")
    @GetMapping("/{userId}/operations")
    public Result<List<AccountOperation>> operations(@PathVariable("userId") Long userId) {
        return Result.success(operations.list(userId));
    }

    @Operation(summary = "修改账号基础资料")
    @PutMapping("/{userId}/profile")
    public Result<Void> profile(@AuthenticationPrincipal LoginPrincipal actor, @PathVariable("userId") Long target,
                                @Valid @RequestBody StaffProfileDTO dto) {
        profiles.updateManaged(actor.userId(), target, dto);
        return Result.success();
    }

    @Operation(summary = "查询平台用户")
    @GetMapping
    public Result<List<PlatformUserViewVO>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "roleCode", required = false) String roleCode,
            @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return Result.success(platformUserService.list(keyword, roleCode, enabled));
    }

    @Operation(summary = "强制用户退出登录")
    @PostMapping("/{userId}/force-logout")
    /**
     * @param userId 目标用户ID
     * @return 操作结果
     * @apiNote 平台管理员强制用户退出登录
     */
    public Result<Void> forceLogout(@AuthenticationPrincipal LoginPrincipal actor, @PathVariable("userId") Long userId,
                                    @Valid @RequestBody(required = false) AccountOperationDTO dto) {
        platformUserService.forceLogout(actor.userId(), userId, dto == null ? null : dto.getReason());
        return Result.success();
    }

    @Operation(summary = "停用或启用普通用户、主办方管理员账号")
    @PatchMapping("/{userId}/enabled")
    public Result<Void> updateEnabled(@AuthenticationPrincipal LoginPrincipal actor,
                                      @PathVariable("userId") Long userId, @RequestParam("enabled") boolean enabled,
                                      @Valid @RequestBody(required = false) AccountOperationDTO dto) {
        platformUserService.updateEnabled(actor.userId(), userId, enabled, dto == null ? null : dto.getReason());
        return Result.success();
    }
}
