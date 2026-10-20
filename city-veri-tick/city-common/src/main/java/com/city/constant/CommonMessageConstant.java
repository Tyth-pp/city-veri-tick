package com.city.constant;

/**
 * 通用响应提示文案。
 */
public final class CommonMessageConstant {

    private CommonMessageConstant() {
    }

    public static final String SUCCESS = "Success";

    public static final String BAD_REQUEST = "Bad request";

    public static final String UNAUTHORIZED = "Unauthorized";

    public static final String FORBIDDEN = "Forbidden";

    public static final String NOT_FOUND = "Resource not found";

    public static final String CONFLICT = "Conflict";

    public static final String INTERNAL_ERROR = "Internal server error";

    public static final String SERVICE_UNAVAILABLE = "服务暂时不可用，请稍后重试；如操作结果未确认，请刷新后查看。";

    public static final String DUPLICATE_RESOURCE = "账号、手机号或机构信用代码已存在，请检查后重试。";
}
