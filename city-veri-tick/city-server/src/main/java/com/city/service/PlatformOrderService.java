package com.city.service;

import com.city.vo.OrderViewVO;
import com.city.vo.OrganizerVerificationRecordVO;
import com.city.vo.OrganizerVerificationSummaryVO;

import java.util.List;

public interface PlatformOrderService {
    List<OrderViewVO> list(String status);

    OrderViewVO detail(Long orderId);

    OrganizerVerificationSummaryVO verificationSummary();

    List<OrganizerVerificationRecordVO> verificationRecords(Boolean checkedIn);
}
