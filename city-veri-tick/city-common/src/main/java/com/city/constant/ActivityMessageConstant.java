package com.city.constant;

/**
 * 活动、场次与售票配置提示文案。
 */
public final class ActivityMessageConstant {

    private ActivityMessageConstant() {
    }

    public static final String SESSION_TIME_INVALID = "场次结束时间必须晚于开始时间";

    public static final String SESSION_TIME_REQUIRED = "请完整填写场次开始和结束时间";

    public static final String SESSION_CAPACITY_INVALID = "场次容量必须是大于 0 的整数";

    public static final String SALE_TIME_REQUIRED = "请先设置本场次的统一售票开始和结束时间";

    public static final String SALE_TIME_INVALID = "售票结束时间必须晚于售票开始时间";

    public static final String SALE_TIME_AFTER_SESSION = "售票结束时间不能晚于场次开始时间";

    public static final String ACTIVITY_SESSION_REQUIRED = "提交审核前请至少创建一个活动场次";

    public static final String SESSION_CAPACITY_EXCEEDED = "票种库存总和不能超过场次容量";

    public static final String SESSION_NOT_ON_SALE = "该场次当前未开售";

    public static final String ACTIVITY_NOT_PUBLISHED = "该活动当前未发布，暂不可购买";

    public static final String ACTIVITY_QUERY_BUSY = "活动访问较多，请稍后重试。";

    public static final String SESSION_START_REQUIRED = "请选择场次开始时间";

    public static final String SESSION_END_REQUIRED = "请选择场次结束时间";

    public static final String SESSION_CAPACITY_REQUIRED = "请填写场次容量";

    public static final String REJECTION_REASON_TOO_LONG = "驳回原因不能超过500字";

    public static final String ID_INVALID = "活动编号必须为正整数";

    public static final String OFFLINE_REASON_REQUIRED = "请填写500字以内的下架原因";
}
