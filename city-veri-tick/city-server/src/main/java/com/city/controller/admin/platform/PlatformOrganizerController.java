package com.city.controller.admin.platform;

import com.city.security.LoginPrincipal;
import com.city.service.StaffAccountService;
import com.city.dto.CreateOrganizerDTO;
import com.city.dto.StaffAccountDTO;
import com.city.vo.StaffAccountViewVO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.city.result.PageResult;
import com.city.result.Result;
import com.city.service.PlatformOrganizerService;
import com.city.vo.OrganizerAdminViewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "平台主办方接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/platform/organizers")
public class PlatformOrganizerController {
    @Autowired
    private StaffAccountService staff;

    @Autowired
    private PlatformOrganizerService platformOrganizerService;

    @Operation(summary = "创建主办方及初始管理员账号")
    @PostMapping
    public Result<Long> create(@AuthenticationPrincipal LoginPrincipal p,
                               @Valid @RequestBody CreateOrganizerDTO dto) {
        return Result.success(staff.createOrganizer(p.userId(), dto));
    }

    @Operation(summary = "创建主办方管理员账号")
    @PostMapping("/{id}/administrators")
    public Result<Long> administrator(@PathVariable("id") Long id, @Valid @RequestBody StaffAccountDTO dto) {
        return Result.success(staff.createAdministrator(id, dto));
    }

    @Operation(summary = "查询主办方所属账号")
    @GetMapping("/{id}/accounts")
    public Result<List<StaffAccountViewVO>> accounts(@PathVariable("id") Long id) {
        platformOrganizerService.detail(id);
        return Result.success(staff.accounts(id, null));
    }

    @Operation(summary = "查询主办方活动列表")
    @GetMapping("/{id}/activities")
    public Result<List<Map<String, Object>>> activities(@PathVariable("id") Long id) {
        platformOrganizerService.detail(id);
        return Result.success(staff.activityOptions(id));
    }

    @Operation(summary = "分页查询主办方")
    @GetMapping
    public Result<PageResult<OrganizerAdminViewVO>> page(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(platformOrganizerService.page(keyword, status, page, pageSize));
    }

    @Operation(summary = "查询主办方详情")
    @GetMapping("/{id}")
    public Result<OrganizerAdminViewVO> detail(@PathVariable("id") Long id) {
        return Result.success(platformOrganizerService.detail(id));
    }
}
