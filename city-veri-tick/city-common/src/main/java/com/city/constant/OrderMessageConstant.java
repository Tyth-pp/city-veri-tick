package com.city.constant;

/**
 * 订单与支付提示文案。
 */
public final class OrderMessageConstant {

    private OrderMessageConstant() {
    }

    public static final String ORDER_PAYMENT_EXPIRED = "订单已超时，请重新选票下单";

    public static final String ORDER_REQUEST_BUSY = "订单请求正在处理中，请稍后查看订单。";

    public static final String IDEMPOTENCY_KEY_INVALID = "Idempotency-Key 长度必须为 8 到 128 个字符";

    public static final String QUANTITY_INVALID = "购票数量不合法";
}
