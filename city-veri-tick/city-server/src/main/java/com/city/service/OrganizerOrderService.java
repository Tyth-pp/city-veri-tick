package com.city.service;

import com.city.vo.OrderViewVO;
import com.city.vo.OrganizerVerificationRecordVO;
import com.city.vo.OrganizerVerificationSummaryVO;

import java.util.List;

public interface OrganizerOrderService {
    List<OrderViewVO> list(Long userId, String status);

    OrderViewVO detail(Long userId, Long orderId);

    OrganizerVerificationSummaryVO verificationSummary(Long userId);

    List<OrganizerVerificationRecordVO> verificationRecords(Long userId, Boolean checkedIn);
}
