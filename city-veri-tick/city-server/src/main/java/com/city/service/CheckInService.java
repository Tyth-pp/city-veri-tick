package com.city.service;

import com.city.vo.VerifyTicketViewVO;
import com.city.vo.VerifierStatisticsVO;

import java.util.List;

public interface CheckInService {
    /**
     * @param verifierId 当前登录核销员ID
     * @param code       门票核销码
     * @param idCardNo   待核对的观演人身份证号
     * @return 核销结果和脱敏身份信息
     * @apiNote 校验并核销门票
     */
    VerifyTicketViewVO checkIn(Long verifierId, String code, String idCardNo);

    VerifyTicketViewVO detail(Long verifierId, String code);

    List<VerifyTicketViewVO> records(Long verifierId);

    VerifierStatisticsVO statistics(Long verifierId);
}
