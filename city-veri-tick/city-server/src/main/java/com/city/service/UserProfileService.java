package com.city.service;

import com.city.dto.UpdateProfileDTO;
import com.city.vo.UserProfileVO;

/**
 * 用户个人资料及实名信息管理的业务契约。
 */
public interface UserProfileService {
    UserProfileVO get(Long userId);

    UserProfileVO update(Long userId, UpdateProfileDTO request);
}
