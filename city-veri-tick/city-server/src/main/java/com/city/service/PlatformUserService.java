package com.city.service;

import com.city.vo.PlatformUserViewVO;

import java.util.List;

public interface PlatformUserService {
    List<PlatformUserViewVO> list(String keyword, String roleCode, Boolean enabled);

    void updateEnabled(Long actorId, Long userId, boolean enabled);

    void forceLogout(Long actorId, Long userId);

    void updateEnabled(Long actorId, Long userId, boolean enabled, String reason);

    void forceLogout(Long actorId, Long userId, String reason);
}
