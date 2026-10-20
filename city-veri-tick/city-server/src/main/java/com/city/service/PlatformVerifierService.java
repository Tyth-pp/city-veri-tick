package com.city.service;

import com.city.dto.AssignVerifierDTO;
import com.city.vo.PlatformVerifierViewVO;

import java.util.List;

public interface PlatformVerifierService {
    List<PlatformVerifierViewVO> list(Long activityId, Boolean enabled);

    void assign(Long assignedBy, AssignVerifierDTO dto);

    void updateEnabled(Long id, boolean enabled);
}
