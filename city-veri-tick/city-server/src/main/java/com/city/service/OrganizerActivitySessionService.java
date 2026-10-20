package com.city.service;

import com.city.dto.CreateActivitySessionDTO;
import com.city.dto.UpdateActivitySessionDTO;
import com.city.vo.ActivitySessionViewVO;

import java.util.List;

public interface OrganizerActivitySessionService {
    /**
     * @param userId 当前登录用户ID
     * @param activityId 活动ID
     * @param dto 创建场次请求参数
     * @return 创建的场次
     * @apiNote 为本主办方活动创建场次
     */
    ActivitySessionViewVO create(Long userId, Long activityId, CreateActivitySessionDTO dto);

    /**
     * @param userId 当前登录用户ID
     * @param activityId 活动ID
     * @return 本活动的场次列表
     * @apiNote 查询本主办方活动场次
     */
    List<ActivitySessionViewVO> list(Long userId, Long activityId);

    /**
     * @param userId 当前登录用户ID
     * @param sessionId 场次ID
     * @param dto 修改场次请求参数
     * @return 修改后的场次
     * @apiNote 修改本主办方活动场次
     */
    ActivitySessionViewVO update(Long userId, Long sessionId, UpdateActivitySessionDTO dto);

    /**
     * @param userId 当前登录用户ID
     * @param sessionId 场次ID
     * @return 无返回值
     * @apiNote 删除本主办方活动场次
     */
    void delete(Long userId, Long sessionId);
}
