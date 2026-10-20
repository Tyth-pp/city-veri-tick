package com.city.controller.admin.platform;
import com.city.vo.PlatformActivityDetailVO;

import com.city.dto.RejectActivityDTO;
import com.city.result.Result;
import com.city.result.PageResult;
import com.city.security.LoginPrincipal;
import com.city.service.PlatformActivityService;
import com.city.vo.ActivityViewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "平台活动接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/platform/activities")
public class PlatformActivityController {
    @Autowired
    private PlatformActivityService platformActivityService;

    @Operation(summary = "查询全部活动列表")
    @GetMapping
    /**
     * @return 平台全部活动列表
     * @apiNote 查询全部活动
     */
    public Result<PageResult<ActivityViewVO>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(platformActivityService.page(keyword, status, page, pageSize));
    }

    @Operation(summary = "查询活动审核详情")
    @GetMapping("/{id}")
    public Result<PlatformActivityDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(platformActivityService.detail(id));
    }

    @Operation(summary = "审核通过并发布活动")
    @PostMapping("/{id}/approve")
    /**
     * @param user 当前登录平台管理员
     * @param id 活动ID
     * @return 操作结果
     * @apiNote 审核通过并发布活动
     */
    public Result<Void> approve(@AuthenticationPrincipal LoginPrincipal user, @PathVariable("id") Long id) {
        platformActivityService.approve(user.userId(), id);
        return Result.success();
    }

    @Operation(summary = "驳回活动")
    @PostMapping("/{id}/reject")
    /**
     * @param user 当前登录平台管理员
     * @param id 活动ID
     * @param dto 驳回活动请求参数
     * @return 操作结果
     * @apiNote 驳回活动并记录原因
     */
    public Result<Void> reject(@AuthenticationPrincipal LoginPrincipal user, @PathVariable("id") Long id,
                               @Valid @RequestBody RejectActivityDTO dto) {
        platformActivityService.reject(user.userId(), id, dto.getReason());
        return Result.success();
    }

    @Operation(summary = "下架已发布活动")
    @PostMapping("/{id}/offline")
    /**
     * @param user 当前登录平台管理员
     * @param id 活动ID
     * @return 操作结果
     * @apiNote 下架已发布活动
     */
    public Result<Void> offline(@AuthenticationPrincipal LoginPrincipal user, @PathVariable("id") Long id,@Valid @RequestBody RejectActivityDTO dto) {
        platformActivityService.offline(user.userId(), id,dto.getReason());
        return Result.success();
    }
}
