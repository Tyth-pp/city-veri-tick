package com.city.controller.admin.organizer;

import com.city.dto.CreateActivitySessionDTO;
import com.city.dto.UpdateActivitySessionDTO;
import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.OrganizerActivitySessionService;
import com.city.vo.ActivitySessionViewVO;
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
@RequestMapping("/controller/admin/organizer")
public class OrganizerActivitySessionController {
    @Autowired
    private OrganizerActivitySessionService organizerActivitySessionService;

    /**
     * @param user 当前登录用户
     * @param activityId 活动ID
     * @return 本活动场次列表
     * @apiNote 查询活动场次
     */
    @Operation(summary = "查询活动场次")
    @GetMapping("/activities/{activityId}/sessions")
    public Result<List<ActivitySessionViewVO>> list(@AuthenticationPrincipal LoginPrincipal user,
                                                    @PathVariable("activityId") Long activityId) {
        return Result.success(organizerActivitySessionService.list(user.userId(), activityId));
    }

    /**
     * @param user 当前登录用户
     * @param activityId 活动ID
     * @param dto 创建场次请求参数
     * @return 创建的活动场次
     * @apiNote 创建活动场次
     */
    @Operation(summary = "创建活动场次")
    @PostMapping("/activities/{activityId}/sessions")
    public Result<ActivitySessionViewVO> create(@AuthenticationPrincipal LoginPrincipal user,
                                                @PathVariable("activityId") Long activityId,
                                                @Valid @RequestBody CreateActivitySessionDTO dto) {
        return Result.success(organizerActivitySessionService.create(user.userId(), activityId, dto));
    }

    /**
     * @param user 当前登录用户
     * @param sessionId 场次ID
     * @param dto 修改场次请求参数
     * @return 修改后的活动场次
     * @apiNote 修改活动场次
     */
    @Operation(summary = "修改活动场次")
    @PutMapping("/sessions/{sessionId}")
    public Result<ActivitySessionViewVO> update(@AuthenticationPrincipal LoginPrincipal user,
                                                @PathVariable("sessionId") Long sessionId,
                                                @Valid @RequestBody UpdateActivitySessionDTO dto) {
        return Result.success(organizerActivitySessionService.update(user.userId(), sessionId, dto));
    }

    /**
     * @param user 当前登录用户
     * @param sessionId 场次ID
     * @return 操作结果
     * @apiNote 删除活动场次
     */
    @Operation(summary = "删除活动场次")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> delete(@AuthenticationPrincipal LoginPrincipal user,
                               @PathVariable("sessionId") Long sessionId) {
        organizerActivitySessionService.delete(user.userId(), sessionId);
        return Result.success();
    }
}
