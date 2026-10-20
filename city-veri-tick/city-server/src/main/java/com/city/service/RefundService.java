package com.city.service;

import com.city.constant.RefundMessageConstant;
import com.city.vo.OrderViewVO;
import com.city.entity.RefundRequest;
import com.city.entity.TicketOrder;

/**
 * 整单退款申请、审核与订单展示补充的业务契约。
 */
public interface RefundService {
    String RULE = RefundMessageConstant.RULE;

    RefundRequest apply(Long userId, Long orderId, String explanation);

    RefundRequest review(Long actor, Long orderId, Long refundId, boolean approved, String explanation);

    void enrich(OrderViewVO view, TicketOrder order, boolean detail);
}
