package com.city.controller.admin.organizer;

import com.city.dto.CreateActivityDTO;
import com.city.dto.UpdateActivityDTO;
import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.OrganizerActivityService;
import com.city.vo.ActivityViewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "主办方活动管理接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/organizer/activities")
public class OrganizerActivityController {
    @Autowired
    private OrganizerActivityService organizerActivityService;
    /**
     * @param user 当前登录用户
     * @return 本主办方活动列表
     * @apiNote 查询本主办方活动列表
     */
    @Operation(summary = "查询本主办方活动列表")
    @GetMapping
    public Result<List<ActivityViewVO>> list(@AuthenticationPrincipal LoginPrincipal user) {
        return Result.success(organizerActivityService.listMine(user.userId()));
    }

    /**
     * @param user 当前登录用户
     * @param dto 创建活动草稿请求参数
     * @return 创建的活动草稿
     * @apiNote 创建活动草稿
     */
    @Operation(summary = "创建活动草稿")
    @PostMapping
    public Result<ActivityViewVO> create(@AuthenticationPrincipal LoginPrincipal user,
                                         @Valid @RequestBody CreateActivityDTO dto) {
        return Result.success(organizerActivityService.create(user.userId(), dto));
    }
    /**
     * @param user 当前登录用户
     * @param id 活动ID
     * @param dto 修改活动草稿请求参数
     * @return 修改后的活动草稿
     * @apiNote 修改活动草稿
     */

    @Operation(summary = "修改活动草稿")
    @PutMapping("/{id}")
    public Result<ActivityViewVO> update(@AuthenticationPrincipal LoginPrincipal user, @PathVariable("id") Long id,
                                         @Valid @RequestBody UpdateActivityDTO dto) {
        return Result.success(organizerActivityService.update(user.userId(), id, dto));

    }
    /**
     * @param user 当前登录用户
     * @param id 活动ID
     * @apiNote 提交活动审核
     */
    @Operation(summary = "提交活动审核")
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@AuthenticationPrincipal LoginPrincipal user, @PathVariable("id") Long id) {
        organizerActivityService.submit(user.userId(), id);
        return Result.success();
    }

    @Operation(summary = "删除活动草稿")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal LoginPrincipal user, @PathVariable("id") Long id) {
        organizerActivityService.delete(user.userId(), id);
        return Result.success();
    }
}
