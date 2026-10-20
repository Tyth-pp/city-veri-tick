package com.city.vo;

import com.city.entity.Coupon;
import lombok.Data;
import java.time.LocalDateTime;

/** 当前账号领取的某一张券；state 是按实际券、使用时间与订单推导的展示状态。 */
@Data
public class UserCouponViewVO {
    private Long userCouponId;
    private Coupon coupon;
    private String group;
    private String state;
    private String stateLabel;
    private String reason;
    private LocalDateTime claimedAt;
    private LocalDateTime usedAt;
    private Long orderId;
    private String orderNo;
    private boolean canPay;
}
