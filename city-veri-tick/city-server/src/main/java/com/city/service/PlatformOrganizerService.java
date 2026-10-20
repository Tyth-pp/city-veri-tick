package com.city.service;

import com.city.result.PageResult;
import com.city.vo.OrganizerAdminViewVO;

public interface PlatformOrganizerService {
    PageResult<OrganizerAdminViewVO> page(String keyword, String status, long page, long pageSize);

    OrganizerAdminViewVO detail(Long organizerId);
}
