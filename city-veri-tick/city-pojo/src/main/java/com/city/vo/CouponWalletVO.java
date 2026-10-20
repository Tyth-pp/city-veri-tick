package com.city.vo;

import lombok.Data;
import java.util.List;

/**
 * 优惠券钱包视图对象，用于展示用户优惠券的各类统计信息
 * 包含优惠券列表和各类优惠券的数量统计
 */
@Data
public class CouponWalletVO {
    // 用户优惠券列表，包含优惠券的详细信息
    private List<UserCouponViewVO> items;
    // 可用优惠券的数量
    private long usableCount;
    // 未使用优惠券的数量
    private long unusedCount;
    // 已使用优惠券的数量
    private long usedCount;
    // 已失效优惠券的数量
    private long invalidCount;
}
