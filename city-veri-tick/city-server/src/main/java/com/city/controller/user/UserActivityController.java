package com.city.controller.user;

import com.city.result.Result;
import com.city.service.UserActivityService;
import com.city.vo.UserActivityDetailVO;
import com.city.vo.UserActivitySummaryVO;
import com.city.vo.UserActivityPreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户活动接口")
@RestController
@RequestMapping("/controller/user/activities")
public class UserActivityController {
    @Autowired
    private UserActivityService userActivityService;

    @Operation(summary = "查询收藏活动预览及当前状态")
    @GetMapping("/{activityId}/preview")
    public Result<UserActivityPreviewVO> getPreview(@PathVariable("activityId") Long activityId) {
        return Result.success(userActivityService.getPreview(activityId));
    }

    /**
     * @param keyword 活动标题关键字，可为空
     * @return 已发布活动列表
     * @apiNote 查询用户可浏览的活动列表
     */
    @Operation(summary = "查询已发布活动列表")
    @GetMapping
    public Result<List<UserActivitySummaryVO>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category) {
        return Result.success(userActivityService.list(keyword, category));
    }

    /**
     * @param activityId 活动ID
     * @return 活动详情及其可售场次、启用票种
     * @apiNote 查询用户可浏览的活动详情
     */
    @Operation(summary = "查询活动详情")
    @GetMapping("/{activityId}")
    public Result<UserActivityDetailVO> getDetail(@PathVariable("activityId") Long activityId) {
        return Result.success(userActivityService.getDetail(activityId));
    }
}
