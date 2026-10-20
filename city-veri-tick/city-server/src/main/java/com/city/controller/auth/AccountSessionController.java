package com.city.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.city.dto.StaffProfileDTO;
import com.city.service.StaffProfileService;
import com.city.vo.StaffProfileVO;

import com.city.dto.ChangePasswordDTO;
import com.city.result.Result;
import com.city.security.*;
import com.city.service.StaffAccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 账户会话控制器，处理用户账户相关的请求，包括个人信息查看与修改、会话状态管理和密码修改等功能
 */
@Tag(name = "账号资料与会话接口")
@RestController
@RequestMapping("/controller/account")
public class AccountSessionController {

    @Autowired
    private StaffAccountService staff;

    @Autowired
    private AccountStatusChecker status;

    @Autowired
    private StaffProfileService profiles;

    /**
     * 获取当前登录用户的个人信息
     * @param p 登录主体信息，包含用户ID等
     * @return 返回包含用户个人信息的Result对象
     */
    @Operation(summary = "查询当前账号资料")
    @GetMapping("/profile")
    public Result<StaffProfileVO> profile(@AuthenticationPrincipal LoginPrincipal p) {
        return Result.success(profiles.self(p.userId()));
    }

    /**
     * 更新当前登录用户的个人信息
     * @param p 登录主体信息，包含用户ID等
     * @param dto 包含更新后个人信息的DTO对象
     * @return 返回更新后的个人信息Result对象
     */
    @Operation(summary = "修改当前账号资料")
    @PutMapping("/profile")
    public Result<StaffProfileVO> profile(@AuthenticationPrincipal LoginPrincipal p, @Valid @RequestBody StaffProfileDTO dto) {
        profiles.updateSelf(p.userId(), dto);
        return Result.success(profiles.self(p.userId()));
    }

    /**
     * 获取当前用户的会话状态信息
     * @param p 登录主体信息，包含用户ID等
     * @return 返回包含用户会话状态信息的Result对象
     */
    @Operation(summary = "查询当前账号会话状态")
    @GetMapping("/session")
    public Result<Map<String, Boolean>> session(@AuthenticationPrincipal LoginPrincipal p) {
        return Result.success(Map.of("mustChangePassword", status.mustChangePassword(p.userId())));
    }

    /**
     * 修改当前登录用户的密码
     * @param p 登录主体信息，包含用户ID等
     * @param dto 包含原密码和新密码的DTO对象
     * @return 返回操作结果的Result对象
     */
    @Operation(summary = "修改当前账号密码")
    @PostMapping("/password")
    public Result<Void> password(@AuthenticationPrincipal LoginPrincipal p, @Valid @RequestBody ChangePasswordDTO dto) {
        staff.changePassword(p.userId(), dto);
        return Result.success();
    }
}

