package com.city.constant;

/**
 * 登录认证与验证码提示文案。
 */
public final class AuthMessageConstant {

    private AuthMessageConstant() {
    }

    public static final String SMS_CHANNEL_UNAVAILABLE = "短信服务尚未配置，请联系平台管理员。";

    public static final String SMS_SEND_TOO_FREQUENT = "验证码发送过于频繁，请稍后再试。";

    public static final String SMS_ATTEMPTS_EXCEEDED = "验证码尝试次数过多，请10分钟后再试。";

    public static final String SMS_VERIFICATION_BUSY = "验证码正在校验，请勿重复提交，稍后重试。";

    public static final String SMS_CODE_INVALID = "验证码输入错误，请重新输入";

    public static final String LOGIN_CREDENTIALS_INVALID = "用户名或密码输入错误";

    public static final String LOGIN_EXPIRED = "登录已过期或失效，请重新登录。";

    public static final String SESSION_ACCOUNT_DISABLED = "该登录会话已因账号停用而失效，请重新登录。";

    public static final String FORCED_LOGOUT = "当前登录已被平台管理员结束，请重新登录。";

    public static final String SESSION_ENDED = "当前登录已结束，请重新登录。";

    public static final String INITIAL_PASSWORD_CHANGED = "初始密码已修改，请使用新密码重新登录。";

    public static final String INITIAL_PASSWORD_CHANGE_REQUIRED = "请先修改初始密码后再使用账号。";

    public static final String SESSION_CHECK_UNAVAILABLE = "登录状态暂时无法验证，请稍后重试。";

    public static final String ACCESS_DENIED = "当前账号没有执行此操作的权限。";
}
