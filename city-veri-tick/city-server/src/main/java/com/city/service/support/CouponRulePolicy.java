package com.city.service.support;

import com.city.constant.CouponMessageConstant;
import com.city.dto.CreateCouponDTO;
import com.city.entity.Coupon;
import com.city.enums.ActivityCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 优惠券配置和金额规则。金额与适用资格由服务端计算。
 */
public final class CouponRulePolicy {
    private CouponRulePolicy() {
    }

    public static void validate(CreateCouponDTO d) {
        if (!Set.of("NORMAL", "LIMITED", "FLASH_SALE").contains(String.valueOf(d.getClaimMode())))
            fail(CouponMessageConstant.CLAIM_MODE_INVALID);
        if (d.getName() == null || d.getName().isBlank() || d.getName().trim().length() > 64)
            fail(CouponMessageConstant.NAME_INVALID);
        if (!Set.of("FULL_REDUCTION", "DISCOUNT", "NO_THRESHOLD").contains(String.valueOf(d.getCouponType())))
            fail(CouponMessageConstant.TYPE_INVALID);
        if (d.getTotalQuantity() == null || d.getTotalQuantity() < 1) fail(CouponMessageConstant.TOTAL_QUANTITY_INVALID);
        if (d.getPerUserLimit() == null || d.getPerUserLimit() < 0 || d.getPerUserLimit() > d.getTotalQuantity())
            fail(CouponMessageConstant.PER_USER_LIMIT_INVALID);
        if (!money(d.getMinPayAmount(), true)) fail(CouponMessageConstant.MINIMUM_PAYMENT_INVALID);
        if (!money(d.getMinOrderAmount(), false)) fail(CouponMessageConstant.THRESHOLD_INVALID);
        if ("DISCOUNT".equals(d.getCouponType())) {
            if (d.getDiscountRate() == null || d.getDiscountRate().compareTo(new BigDecimal("0.01")) < 0 || d.getDiscountRate().compareTo(BigDecimal.ONE) >= 0 || d.getDiscountRate().scale() > 4)
                fail(CouponMessageConstant.DISCOUNT_RATE_INVALID);
            if (d.getMaxDiscountAmount() != null && !money(d.getMaxDiscountAmount(), true))
                fail(CouponMessageConstant.DISCOUNT_CAP_INVALID);
        } else {
            if (!money(d.getDiscountAmount(), true)) fail(CouponMessageConstant.DISCOUNT_AMOUNT_INVALID);
            if ("FULL_REDUCTION".equals(d.getCouponType()) && (d.getMinOrderAmount().signum() <= 0 || d.getMinOrderAmount().compareTo(d.getDiscountAmount()) < 0))
                fail(CouponMessageConstant.REDUCTION_THRESHOLD_INVALID);
        }
        if (d.getStartsAt() == null || d.getEndsAt() == null || !d.getEndsAt().isAfter(d.getStartsAt()))
            fail(CouponMessageConstant.CLAIM_TIME_INVALID);
        if (d.getUseStartsAt() == null || d.getUseEndsAt() == null || !d.getUseEndsAt().isAfter(d.getUseStartsAt()))
            fail(CouponMessageConstant.USE_TIME_INVALID);
        if (d.getUseEndsAt().isBefore(d.getEndsAt())) fail(CouponMessageConstant.USE_END_BEFORE_CLAIM_END);
        if (!Set.of("ALL", "ACTIVITY").contains(String.valueOf(d.getScopeType()))) fail(CouponMessageConstant.SCOPE_TYPE_INVALID);
        if ("ACTIVITY".equals(d.getScopeType()) && (d.getActivityId() == null || d.getActivityId() <= 0))
            fail(CouponMessageConstant.ACTIVITY_REQUIRED);
        Set<String> codes = Arrays.stream(ActivityCategory.values()).map(Enum::name).collect(Collectors.toSet());
        if (d.getCategoryCodes() != null && d.getCategoryCodes().stream().anyMatch(c -> !codes.contains(c)))
            fail(CouponMessageConstant.CATEGORY_INVALID);
        if (!Set.of("RETURN", "NO_RETURN").contains(String.valueOf(d.getRefundPolicy())))
            fail(CouponMessageConstant.REFUND_POLICY_INVALID);
    }

    private static boolean money(BigDecimal n, boolean positive) {
        return n != null && n.scale() <= 2 && n.compareTo(new BigDecimal("99999999.99")) <= 0 && (positive ? n.signum() > 0 : n.signum() >= 0);
    }

    private static void fail(String message) {
        throw new IllegalArgumentException(message);
    }

    public static BigDecimal discount(Coupon c, BigDecimal amount) {
        BigDecimal discount;
        if ("DISCOUNT".equals(c.getCouponType())) {
            discount = amount.multiply(BigDecimal.ONE.subtract(c.getDiscountRate())).setScale(2, RoundingMode.DOWN);
            if (c.getMaxDiscountAmount() != null) discount = discount.min(c.getMaxDiscountAmount());
        } else discount = c.getDiscountAmount();
        BigDecimal floor = c.getMinPayAmount() == null ? new BigDecimal("0.01") : c.getMinPayAmount();
        return discount.min(amount.subtract(floor).max(BigDecimal.ZERO)).max(BigDecimal.ZERO).setScale(2, RoundingMode.DOWN);
    }

    public static LocalDateTime useStart(Coupon c) {
        return c.getUseStartsAt() == null ? c.getStartsAt() : c.getUseStartsAt();
    }

    public static LocalDateTime useEnd(Coupon c) {
        return c.getUseEndsAt() == null ? c.getEndsAt() : c.getUseEndsAt();
    }
}
