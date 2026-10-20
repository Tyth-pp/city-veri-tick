package com.city.service;

import com.city.vo.OrderPaymentPolicyVO;
import com.city.dto.CreateOrderDTO;
import com.city.vo.OrderViewVO;

import java.util.List;

public interface OrderService {
    /**
     * @param userId         当前登录用户ID
     * @param createOrderDTO 创建订单请求参数
     * @return 创建的门票列表
     * @apiNote 创建订单并生成门票
     */
    OrderViewVO create(Long userId, CreateOrderDTO createOrderDTO);

    OrderViewVO create(Long userId, CreateOrderDTO createOrderDTO, String idempotencyKey);

    /**
     * 支付订单方法
     *
     * @param userId  用户ID，用于标识发起支付的用户
     * @param orderId 订单ID，用于标识需要支付的订单
     * @return OrderViewVO 支付后的订单视图对象，包含订单的详细信息
     */
    OrderViewVO pay(Long userId, Long orderId);

    OrderPaymentPolicyVO paymentPolicy();

    OrderViewVO synchronizeExpiry(Long userId, Long orderId);

    OrderViewVO cancel(Long userId, Long orderId);

    int expirePendingOrders();

    /**
     * @param userId 当前登录用户ID
     * @param status 订单状态，可为空
     * @return 当前用户的订单列表
     * @apiNote 查询当前用户订单列表
     */
    List<OrderViewVO> list(Long userId, String status);

    /**
     * @param userId  当前登录用户ID
     * @param orderId 订单ID
     * @return 订单详情
     * @apiNote 查询当前用户订单详情
     */
    OrderViewVO detail(Long userId, Long orderId);
}
