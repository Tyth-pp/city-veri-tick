package com.city.service.support;

import com.city.dto.CreateCouponDTO;
import com.city.entity.Coupon;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class CouponRulePolicyTest {
    private Coupon coupon(String type) {
        Coupon c = new Coupon();
        c.setCouponType(type); c.setDiscountAmount(new BigDecimal("20"));
        c.setDiscountRate(new BigDecimal("0.85")); c.setMaxDiscountAmount(new BigDecimal("100"));
        c.setMinPayAmount(new BigDecimal("0.01")); c.setMinOrderAmount(new BigDecimal("100"));
        return c;
    }
    @Test void discountHasAnUpperLimitAndUsesTwoDecimalPlaces() {
        assertThat(CouponRulePolicy.discount(coupon("DISCOUNT"), new BigDecimal("1000"))).isEqualByComparingTo("100");
        assertThat(CouponRulePolicy.discount(coupon("DISCOUNT"), new BigDecimal("123.45"))).isEqualByComparingTo("18.51");
    }
    @Test void noThresholdCouponStillRequiresMinimumPayment() {
        assertThat(CouponRulePolicy.discount(coupon("NO_THRESHOLD"), new BigDecimal("10"))).isEqualByComparingTo("9.99");
        assertThat(CouponRulePolicy.discount(coupon("NO_THRESHOLD"), BigDecimal.ZERO)).isZero();
    }
    @Test void discountNearFullPricePassesDtoValidationButOneDoesNot() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            CreateCouponDTO d = form();d.setCouponType("DISCOUNT");d.setDiscountAmount(null);
            d.setDiscountRate(new BigDecimal("0.999"));d.setMaxDiscountAmount(new BigDecimal("100"));
            assertThat(factory.getValidator().validate(d)).isEmpty();
            d.setDiscountRate(BigDecimal.ONE);
            assertThat(factory.getValidator().validate(d)).anyMatch(v -> v.getPropertyPath().toString().equals("discountRate"));
        }
    }
    @Test void fullReductionReturnsFixedAmount() {
        assertThat(CouponRulePolicy.discount(coupon("FULL_REDUCTION"), new BigDecimal("120"))).isEqualByComparingTo("20");
    }
    private CreateCouponDTO form() {
        CreateCouponDTO d = new CreateCouponDTO(); d.setName("演出满减"); d.setCouponType("FULL_REDUCTION");
        d.setDiscountAmount(new BigDecimal("20")); d.setMinOrderAmount(new BigDecimal("100"));
        d.setTotalQuantity(10); d.setPerUserLimit(1); d.setScopeType("ALL"); d.setRefundPolicy("RETURN");
        d.setMinPayAmount(new BigDecimal("0.01")); d.setStartsAt(LocalDateTime.of(2030,1,1,0,0));
        d.setEndsAt(d.getStartsAt().plusDays(1)); d.setUseStartsAt(d.getStartsAt()); d.setUseEndsAt(d.getEndsAt().plusDays(2));
        return d;
    }
    @Test void invalidTimeRangeAndUnknownCategoryAreRejected() {
        CreateCouponDTO d = form(); d.setUseEndsAt(d.getUseStartsAt());
        assertThatThrownBy(() -> CouponRulePolicy.validate(d)).hasMessageContaining("使用结束");
        CreateCouponDTO unknown = form(); unknown.setCategoryCodes(java.util.List.of("UNKNOWN"));
        assertThatThrownBy(() -> CouponRulePolicy.validate(unknown)).hasMessageContaining("分类");
    }
    @Test void discountCapMayBeUnlimitedButSpecifiedActivityIsRequired() {
        CreateCouponDTO d = form(); d.setCouponType("DISCOUNT"); d.setDiscountRate(new BigDecimal("0.85"));
        assertThatCode(() -> CouponRulePolicy.validate(d)).doesNotThrowAnyException();
        d.setMaxDiscountAmount(new BigDecimal("100")); d.setScopeType("ACTIVITY");
        assertThatThrownBy(() -> CouponRulePolicy.validate(d)).hasMessageContaining("活动");
        d.setActivityId(7L);
        assertThatCode(() -> CouponRulePolicy.validate(d)).doesNotThrowAnyException();
    }
    @Test void unlimitedCapAndPersonalClaimsRemainBoundedByPaymentAndInventory(){
        var d=form();d.setCouponType("DISCOUNT");d.setDiscountRate(new BigDecimal("0.90"));d.setMaxDiscountAmount(null);d.setPerUserLimit(0);
        assertThatCode(()->CouponRulePolicy.validate(d)).doesNotThrowAnyException();
        var c=coupon("DISCOUNT");c.setDiscountRate(new BigDecimal("0.90"));c.setMaxDiscountAmount(null);
        assertThat(CouponRulePolicy.discount(c,new BigDecimal("10000"))).isEqualByComparingTo("1000");
        c.setDiscountRate(new BigDecimal("0.01"));assertThat(CouponRulePolicy.discount(c,new BigDecimal("0.01"))).isEqualByComparingTo("0");
        d.setTotalQuantity(1000);d.setPerUserLimit(500);assertThatCode(()->CouponRulePolicy.validate(d)).doesNotThrowAnyException();d.setPerUserLimit(-1);assertThatThrownBy(()->CouponRulePolicy.validate(d)).hasMessageContaining("每人");
        d.setPerUserLimit(0);d.setMaxDiscountAmount(BigDecimal.ZERO);assertThatThrownBy(()->CouponRulePolicy.validate(d)).hasMessageContaining("封顶");
    }

}
