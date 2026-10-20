package com.city.service;

import com.city.vo.UserActivityDetailVO;
import com.city.vo.UserActivitySummaryVO;
import com.city.vo.UserActivityPreviewVO;

import java.util.List;

public interface UserActivityService {
    /**
     * @param keyword 活动标题关键字，可为空
     * @return 已发布活动列表
     * @apiNote 查询用户可浏览的已发布活动
     */
    List<UserActivitySummaryVO> list(String keyword, String category);

    default List<UserActivitySummaryVO> list(String keyword) {
        return list(keyword, null);
    }

    /**
     * @param activityId 活动ID
     * @return 活动详情及其可售场次、启用票种
     * @apiNote 查询用户可浏览的活动详情
     */
    UserActivityDetailVO getDetail(Long activityId);

    UserActivityPreviewVO getPreview(Long activityId);
}
