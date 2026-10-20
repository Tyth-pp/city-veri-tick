package com.city.constant;

/**
 * 管理端通知与统计提示文案。
 */
public final class ManagementMessageConstant {

    private ManagementMessageConstant() {
    }

    public static final String DASHBOARD_RANGE_INVALID = "统计范围仅支持7日或30日";

    public static final String MESSAGE_SAVE_FAILED = "业务消息保存失败，操作未完成，请重试";

    public static final String ACTIVITY_SUBMITTED = "新的活动待审核";

    public static final String ACTIVITY_RESUBMITTED = "活动重新提交审核";

    public static final String ACTIVITY_PUBLISHED = "活动审核通过，已发布";

    public static final String ACTIVITY_REJECTED = "活动审核未通过";

    public static final String ACTIVITY_OFFLINE = "活动已由平台下架";

    public static final String REFUND_REQUESTED = "有新的退款申请";

    public static final String SUBMISSION_CONTENT_TEMPLATE = "活动：%s；主办方：%s；负责人：%s";

    public static final String ORGANIZER_ID_TEMPLATE = "机构 ID %s";

    public static final String ACCOUNT_ID_TEMPLATE = "账号 ID %s";

    public static final String ACTIVITY_CONTENT_TEMPLATE = "活动：%s%s";

    public static final String ACTIVITY_DETAILS_SUFFIX = "，请查看活动详情。";

    public static final String ACTIVITY_REASON_TEMPLATE = "；原因：%s";

    public static final String REFUND_CONTENT_TEMPLATE = "活动：%s；申请金额 ¥%s（模拟退款）";
}
