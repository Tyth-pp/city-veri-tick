package com.city.service;

import com.city.dto.ChangePasswordDTO;
import com.city.dto.CreateOrganizerDTO;

import java.util.List;
import java.util.Map;

import com.city.dto.StaffAccountDTO;
import com.city.vo.StaffAccountViewVO;
import com.city.dto.VerifierActivitiesDTO;

/**
 * 主办方及核销员账号创建和权限管理的业务契约。
 */
public interface StaffAccountService {
    Long createOrganizer(Long actor, CreateOrganizerDTO dto);

    Long createAdministrator(Long organizerId, StaffAccountDTO dto);

    Long createVerifier(Long actor, StaffAccountDTO dto);

    List<StaffAccountViewVO> accounts(Long org, String role);

    List<StaffAccountViewVO> verifierAccounts(Long actor);

    List<Map<String, Object>> activityOptions(Long org);

    List<Map<String, Object>> verifierActivityOptions(Long actor);

    void setVerifierEnabled(Long actor, Long userId, boolean enabled);

    void setVerifierEnabled(Long actor, Long userId, boolean enabled, String reason);

    void setVerifierActivities(Long actor, Long userId, VerifierActivitiesDTO dto);

    void changePassword(Long userId, ChangePasswordDTO dto);
}
