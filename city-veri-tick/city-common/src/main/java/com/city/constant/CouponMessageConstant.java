package com.city.constant;

/**
 * 优惠券配置、领取与使用提示文案。
 */
public final class CouponMessageConstant {

    private CouponMessageConstant() {
    }

    public static final String COUPON_UNAVAILABLE = "优惠券不可用、已过期或库存不足";

    public static final String COUPON_ALREADY_CLAIMED = "该优惠券已领取";

    public static final String COUPON_CLAIM_LIMIT = "已达到该优惠券每人限领数量";

    public static final String COUPON_CLAIM_NOT_STARTED = "优惠券尚未开始领取，请在开抢时间后重试";

    public static final String COUPON_CLAIM_ENDED = "优惠券领取已结束";

    public static final String COUPON_SOLD_OUT = "优惠券已抢光";

    public static final String COUPON_CLAIM_BUSY = "领券人数较多或服务暂不可用，请稍后重试；可在我的优惠券确认领取结果";

    public static final String NAME_REQUIRED = "请填写优惠券名称";

    public static final String ACTIVITY_NOT_FOUND = "指定活动不存在，请重新选择";

    public static final String STATUS_INVALID = "优惠券状态不合法";

    public static final String IDENTITY_INVALID = "优惠券或账号编号不合法";

    public static final String LABEL_INVALID = "已失效";

    public static final String INFORMATION_INVALID = "优惠券信息已失效";

    public static final String LABEL_PENDING = "状态待确认";

    public static final String ORDER_STATE_UPDATING = "订单关联状态更新中，请刷新后查看";

    public static final String LABEL_LOCKED = "订单占用";

    public static final String LOCKED_BY_UNPAID_ORDER = "待付款订单占用，取消或超时后释放";

    public static final String WAITING_FOR_RELEASE = "订单已超时，等待系统释放，请稍后刷新";

    public static final String LABEL_USED = "已使用";

    public static final String NOT_RETURNED_AFTER_REFUND = "订单已退款，本券未退回";

    public static final String APPLIED_TO_ORDER = "优惠已抵扣至关联订单";

    public static final String LABEL_EXPIRED = "已过期";

    public static final String USE_PERIOD_ENDED = "已超过使用有效期";

    public static final String NOT_USABLE = "当前优惠券不可使用";

    public static final String LABEL_DISABLED = "已停用";

    public static final String DISABLED_BY_PLATFORM = "该优惠券已被平台停用";

    public static final String LABEL_NOT_STARTED = "待生效";

    public static final String USE_NOT_STARTED = "尚未到使用时间";

    public static final String LABEL_AVAILABLE = "可使用";

    public static final String SELECTION_HINT = "购票时可在确认订单中选择，是否适用以订单校验为准";

    public static final String ACTIVITY_REMOVED = "活动已移除";

    public static final String DISABLED = "优惠券已停用";

    public static final String EXPIRED = "优惠券已过期";

    public static final String ACTIVITY_SCOPE_MISMATCH = "仅限指定活动使用";

    public static final String CATEGORY_SCOPE_MISMATCH = "不适用于当前活动分类";

    public static final String THRESHOLD_NOT_MET = "未达到优惠券使用门槛";

    public static final String MINIMUM_PAYMENT_NOT_MET = "订单金额未超过最低实付金额";

    public static final String CLAIM_MODE_INVALID = "请选择有效的发放方式";

    public static final String NAME_INVALID = "请填写不超过64字的优惠券名称";

    public static final String TYPE_INVALID = "请选择有效的券类型";

    public static final String TOTAL_QUANTITY_INVALID = "发放总数量必须大于0";

    public static final String PER_USER_LIMIT_INVALID = "每人限领须为正整数且不能超过发放总数量；0表示不限次数";

    public static final String MINIMUM_PAYMENT_INVALID = "最低实付金额至少为0.01元，最多两位小数";

    public static final String THRESHOLD_INVALID = "满减门槛金额不合法";

    public static final String DISCOUNT_RATE_INVALID = "折扣比例必须大于0且小于1";

    public static final String DISCOUNT_CAP_INVALID = "请设置合法的优惠封顶金额";

    public static final String DISCOUNT_AMOUNT_INVALID = "优惠金额必须大于0且最多两位小数";

    public static final String REDUCTION_THRESHOLD_INVALID = "满减门槛必须大于0且不能小于优惠金额";

    public static final String CLAIM_TIME_INVALID = "领券结束时间必须晚于开始时间";

    public static final String USE_TIME_INVALID = "使用结束时间必须晚于开始时间";

    public static final String USE_END_BEFORE_CLAIM_END = "使用结束时间不能早于领券结束时间";

    public static final String SCOPE_TYPE_INVALID = "请选择有效的适用范围";

    public static final String ACTIVITY_REQUIRED = "请选择指定活动";

    public static final String CATEGORY_INVALID = "适用活动分类不合法";

    public static final String REFUND_POLICY_INVALID = "请选择有效的退票后处理规则";
}
