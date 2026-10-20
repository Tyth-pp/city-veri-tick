package com.city.service;

public interface OrganizerAccessService {
    /**
     * @param userId 当前登录用户ID
     * @return 主办方ID
     * @apiNote 查询当前用户所属且启用的主办方
     */
    Long requireOrganizerId(Long userId);
}
