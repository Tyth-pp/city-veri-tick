package com.city.service;

import com.city.vo.ActivityViewVO;
import com.city.vo.PlatformActivityDetailVO;
import com.city.result.PageResult;

import java.util.List;

public interface PlatformActivityService {
    /**
     * @return 平台全部活动列表
     * @apiNote 查询全部活动
     */
    List<ActivityViewVO> list();

    PageResult<ActivityViewVO> page(String keyword, String status, long page, long pageSize);

    PlatformActivityDetailVO detail(Long activityId);

    /**
     * @param reviewerId 当前登录平台管理员ID
     * @param activityId 活动ID
     * @return 无返回值
     * @apiNote 审核通过并发布活动
     */
    void approve(Long reviewerId, Long activityId);

    /**
     * @param reviewerId 当前登录平台管理员ID
     * @param activityId 活动ID
     * @param reason 驳回原因
     * @return 无返回值
     * @apiNote 驳回活动并记录审核原因
     */
    void reject(Long reviewerId, Long activityId, String reason);

    /**
     * @param reviewerId 当前登录平台管理员ID
     * @param activityId 活动ID
     * @return 无返回值
     * @apiNote 下架已发布活动
     */
    void offline(Long reviewerId, Long activityId);
    void offline(Long reviewerId, Long activityId, String reason);
}
