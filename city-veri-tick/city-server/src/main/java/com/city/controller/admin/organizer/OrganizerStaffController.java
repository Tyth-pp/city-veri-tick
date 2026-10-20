package com.city.controller.admin.organizer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.city.service.StaffProfileService;
import com.city.dto.*;
import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.StaffAccountService;
import com.city.vo.StaffAccountViewVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 组织者工作人员控制器
 * 提供组织者工作人员相关的REST API，包括创建、更新、查询等功能
 */
@Tag(name = "主办方核销员管理接口")
@RestController
@RequestMapping("/controller/admin/organizer/verifiers")
public class OrganizerStaffController {
    // 自动注入工作人员账户服务
    @Autowired
    private StaffAccountService staff;
    // 自动注入工作人员档案服务
    @Autowired
    private StaffProfileService profiles;

    /**
     * 更新工作人员档案信息
     * @param p 当前登录用户信息
     * @param id 工作人员ID
     * @param dto 档案数据传输对象
     * @return 操作结果
     */
    @Operation(summary = "修改核销员资料")
    @PutMapping("/{id}/profile")
    public Result<Void> profile(@AuthenticationPrincipal LoginPrincipal p, @PathVariable("id") Long id, @Valid @RequestBody StaffProfileDTO dto) {
        profiles.updateVerifier(p.userId(), id, dto);
        return Result.success();
    }

    /**
     * 获取工作人员列表
     * @param p 当前登录用户信息
     * @return 工作人员列表视图对象
     */
    @Operation(summary = "查询本主办方核销员账号")
    @GetMapping
    public Result<List<StaffAccountViewVO>> list(@AuthenticationPrincipal LoginPrincipal p) {
        return Result.success(staff.verifierAccounts(p.userId()));
    }

    /**
     * 获取工作人员可选活动列表
     * @param p 当前登录用户信息
     * @return 活动列表，以Map形式存储
     */
    @Operation(summary = "查询核销员可分配活动")
    @GetMapping("/activities")
    public Result<List<Map<String, Object>>> activities(@AuthenticationPrincipal LoginPrincipal p) {
        return Result.success(staff.verifierActivityOptions(p.userId()));
    }

    /**
     * 创建新的工作人员账户
     * @param p 当前登录用户信息
     * @param dto 工作人员账户数据传输对象
     * @return 新创建的工作人员ID
     */
    @Operation(summary = "创建核销员账号")
    @PostMapping
    public Result<Long> create(@AuthenticationPrincipal LoginPrincipal p, @Valid @RequestBody StaffAccountDTO dto) {
        return Result.success(staff.createVerifier(p.userId(), dto));
    }

    /**
     * 设置工作人员账户启用/禁用状态
     * @param p 当前登录用户信息
     * @param id 工作人员ID
     * @param enabled 是否启用
     * @param dto 操作数据传输对象（包含操作原因）
     * @return 操作结果
     */
    @Operation(summary = "启用或停用核销员账号")
    @PatchMapping("/{id}/enabled")
    public Result<Void> enabled(@AuthenticationPrincipal LoginPrincipal p, @PathVariable("id") Long id, @RequestParam("enabled") boolean enabled, @Valid @RequestBody(required = false) AccountOperationDTO dto) {
        staff.setVerifierEnabled(p.userId(), id, enabled, dto == null ? null : dto.getReason());
        return Result.success();
    }

    /**
     * 为工作人员分配活动权限
     * @param p 当前登录用户信息
     * @param id 工作人员ID
     * @param dto 活动分配数据传输对象
     * @return 操作结果
     */
    @Operation(summary = "分配核销员活动权限")
    @PutMapping("/{id}/activities")
    public Result<Void> assign(@AuthenticationPrincipal LoginPrincipal p, @PathVariable("id") Long id, @Valid @RequestBody VerifierActivitiesDTO dto) {
        staff.setVerifierActivities(p.userId(), id, dto);
        return Result.success();
    }
}

