package com.city.service;

import com.city.vo.CouponChoiceVO;
import com.city.vo.CouponWalletVO;
import com.city.entity.UserCoupon;
import com.city.entity.Coupon;
import com.city.dto.CreateCouponDTO;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    Coupon create(CreateCouponDTO dto);

    List<Coupon> adminList();

    void setStatus(Long id, String status);

    UserCoupon claim(Long userId, Long couponId);

    List<UserCoupon> list(Long userId);

    CouponWalletVO mine(Long userId);

    List<Coupon> available(Long userId);

    BigDecimal lockForOrder(Long userId, Long couponId, Long orderId, BigDecimal amount, String category, Long activityId);

    List<CouponChoiceVO> choices(Long userId, Long ticketTypeId, int quantity);

    void use(Long userId, Long couponId, Long orderId);

    void release(Long userId, Long couponId, Long orderId);

    void restoreAfterRefund(Long userId, Long couponId, Long orderId);
}
