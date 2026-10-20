package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("coupon")
public class Coupon implements Serializable {
    @TableId
    private Long id;
    private String name;
    private String couponType;
    private String claimMode = "NORMAL";
    private String description;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private BigDecimal minPayAmount;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Integer perUserLimit;
    private String scopeType;
    private Long activityId;
    private String refundPolicy;
    private String status;
    // startsAt/endsAt 保留为领取窗口，兼容已有接口。
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime useStartsAt;
    private LocalDateTime useEndsAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableField(exist = false)
    private List<String> categoryCodes;
    @TableField(exist = false)
    private String activityTitle;
    @TableField(exist = false)
    private Integer userClaimedQuantity;
    @TableField(exist = false)
    private String claimState;
    @TableField(exist = false)
    private java.time.Instant serverTime;
}
