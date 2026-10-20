package com.city.service;

import com.city.dto.StaffProfileDTO;
import com.city.vo.StaffProfileVO;

/**
 * 后台员工资料管理的业务契约。
 */
public interface StaffProfileService {
    StaffProfileVO self(Long id);

    void updateSelf(Long actor, StaffProfileDTO dto);

    void updateManaged(Long actor, Long target, StaffProfileDTO dto);

    void updateVerifier(Long actor, Long target, StaffProfileDTO dto);
}
