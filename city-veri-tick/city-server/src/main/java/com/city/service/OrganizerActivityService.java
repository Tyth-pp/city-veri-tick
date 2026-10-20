package com.city.service;

import com.city.dto.CreateActivityDTO;
import com.city.dto.UpdateActivityDTO;
import com.city.vo.ActivityViewVO;

import java.util.List;

public interface OrganizerActivityService {
    /**
     * @param userId 当前登录用户ID
     * @param dto    创建活动草稿请求参数
     * @return 创建的活动草稿
     * @apiNote 创建本主办方活动草稿
     */
    ActivityViewVO create(Long userId, CreateActivityDTO dto);

    /**
     * @param userId     当前登录用户ID
     * @param activityId 活动ID
     * @param dto        修改活动草稿请求参数
     * @return 修改后的活动草稿
     * @apiNote 修改本主办方活动草稿
     */
    ActivityViewVO update(Long userId, Long activityId, UpdateActivityDTO dto);

    /**
     * @param userId     当前登录用户ID
     * @param activityId 活动ID
     * @return 无返回值
     * @apiNote 提交本主办方活动审核
     */
    void submit(Long userId, Long activityId);

    void delete(Long userId, Long activityId);

    /**
     * @param userId 当前登录用户ID
     * @return 本主办方活动列表
     * @apiNote 查询本主办方活动列表
     */
    List<ActivityViewVO> listMine(Long userId);
}
