package com.city.constant;

/**
 * 退票申请、审核与规则提示文案。
 */
public final class RefundMessageConstant {

    private RefundMessageConstant() {
    }

    public static final String REASON_REQUIRED = "请填写退票原因";

    public static final String REASON_TOO_LONG = "退票原因不能超过500字";

    public static final String REVIEW_RESULT_REQUIRED = "请选择审核结果";

    public static final String REVIEW_EXPLANATION_TOO_LONG = "审核说明不能超过500字";

    public static final String RULE = "支持整单退票：全部电子票未核销，且距场次开始至少48小时。申请审核期间暂停入场；审核通过后按实付金额全额模拟退款。";

    public static final String ORDER_NOT_OWNED_BY_USER = "订单不存在或不属于当前账号";

    public static final String ORDER_CHANGED_RETRY = "订单状态已变化，请刷新后重试";

    public static final String APPLICATION_SAVE_FAILED = "申请保存失败，请重试";

    public static final String ACTIVITY_INCOMPLETE = "活动信息不完整，申请未完成";

    public static final String REQUEST_NOT_FOUND = "退款申请不存在";

    public static final String REQUEST_ALREADY_PROCESSED = "该退款申请已处理，请刷新查看结果";

    public static final String TICKET_STATE_INVALID = "票券状态异常，无法处理退款，请核对核销记录";

    public static final String AMOUNT_MISMATCH = "退款金额与订单实付金额不一致，无法处理";

    public static final String TICKETS_CHANGED = "票券状态已变化，退款未完成";

    public static final String STOCK_RESTORE_FAILED = "库存回补失败，退款未完成";

    public static final String ORDER_CHANGED_INCOMPLETE = "订单状态已变化，处理未完成";

    public static final String REVIEWER_NOT_FOUND = "审核账号不存在，处理未完成";

    public static final String REVIEW_SAVE_FAILED = "审核记录保存失败，处理未完成";

    public static final String ORDER_NOT_OWNED_BY_ORGANIZER = "订单不存在或不属于当前主办方";

    public static final String REQUEST_IN_PROGRESS = "退款申请处理中，请勿重复提交";

    public static final String ORDER_ALREADY_REFUNDED = "订单已退款";

    public static final String ORDER_NOT_PAID = "仅已支付订单可申请退票";

    public static final String PAYMENT_AMOUNT_INVALID = "订单实付金额异常，暂不能申请退票";

    public static final String TICKETS_INCOMPLETE = "电子票记录不完整，暂不能申请退票";

    public static final String TICKET_ALREADY_USED = "订单中已有电子票核销，不支持整单退票";

    public static final String TICKET_STATUS_UNSUPPORTED = "订单票券状态不支持退票";

    public static final String SESSION_TIME_INCOMPLETE = "场次时间不完整，暂不能申请退票";

    public static final String TOO_CLOSE_TO_SESSION = "距场次开始不足48小时，不符合当前退票规则";

    public static final String APPLICATION_REASON_LABEL = "退票原因";

    public static final String REVIEW_EXPLANATION_LABEL = "审核说明";

    public static final String REJECTION_REASON_LABEL = "拒绝原因";

    public static final String REQUIRED_TEMPLATE = "请填写%s";

    public static final String TOO_LONG_TEMPLATE = "%s不能超过500字";
}
