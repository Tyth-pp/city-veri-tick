package com.city.constant;

/**
 * 票种配置与门票核销提示文案。
 */
public final class TicketMessageConstant {

    private TicketMessageConstant() {
    }

    public static final String TICKET_FIELDS_INVALID = "请填写票种名称，票价不能为负数且最多保留两位小数，库存须为不小于 0 的整数";

    public static final String TICKET_TYPE_REQUIRED = "每个活动场次至少需要配置一个启用票种";

    public static final String TICKET_TYPE_NAME_DUPLICATE = "同一场次下票种名称不能重复";

    public static final String TICKET_TYPE_HAS_ORDERS = "已有订单的票种不能删除";

    public static final String TICKET_TYPE_NOT_ON_SALE = "票种当前不可购买，请检查票种状态、库存和销售时间";

    public static final String TICKET_STOCK_NOT_ENOUGH = "票种库存不足";

    public static final String TICKET_ALREADY_CHECKED_IN = "Ticket has already been checked in";

    public static final String NAME_REQUIRED = "请填写票种名称";

    public static final String PRICE_REQUIRED = "请填写票价";

    public static final String PRICE_NEGATIVE = "票价不能小于 0";

    public static final String PRICE_PRECISION_INVALID = "票价最多保留两位小数，且不能超过 99999999.99";

    public static final String STOCK_REQUIRED = "请填写库存";

    public static final String STOCK_NEGATIVE = "库存不能小于 0";

    public static final String STATUS_REQUIRED = "请选择票种启用状态";

    public static final String ORDER_REFUNDING = "该订单退款审核中，电子票暂停核销";

    public static final String ORDER_NOT_PAYABLE_FOR_CHECK_IN = "订单未支付或已退款，电子票不可核销";

    public static final String RECORD_NOT_FOUND = "票券记录不存在，请刷新后重试";

    public static final String CHECK_IN_SUCCESS = "门票核销成功";

    public static final String ALREADY_CHECKED_IN = "门票已核销";

    public static final String CHECK_IN_REFUNDING = "退款审核中，电子票暂停核销";

    public static final String CHECK_IN_REFUNDED = "已退款，电子票不可核销";

    public static final String CHECK_IN_AVAILABLE = "门票可核销";

    public static final String CHECK_IN_ORDER_STATUS_INVALID = "订单状态不支持核销";
}
