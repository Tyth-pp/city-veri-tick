package com.city.constant;

/**
 * 账号、密码与身份信息提示文案。
 */
public final class AccountMessageConstant {

    private AccountMessageConstant() {
    }

    public static final String ACCOUNT_DISABLED = "账号已停用，暂时无法使用，请联系平台管理员。";

    public static final String REAL_NAME_REQUIRED = "请先完成实名信息后再购票";

    public static final String ID_CARD_MISMATCH = "核销码与身份证号不匹配";

    public static final String ACCOUNT_DUPLICATE = "登录账号或手机号已被使用，请更换";

    public static final String PASSWORD_INVALID = "当前密码不正确";

    public static final String PASSWORD_UNCHANGED = "新密码不能与初始密码相同";

    public static final String OPERATION_REASON_TOO_LONG = "操作原因不能超过500字";

    public static final String NEW_PASSWORD_FORMAT_INVALID = "新密码需包含字母和数字，长度10–64位";

    public static final String NEW_PASSWORD_BYTES_EXCEEDED = "新密码的 UTF-8 长度不能超过72字节";

    public static final String CONTACT_PHONE_INVALID = "联系电话格式不正确";

    public static final String PHONE_INVALID = "手机号格式不正确";

    public static final String PHONE_REQUIRED_OR_INVALID = "请输入有效的手机号";

    public static final String USERNAME_FORMAT_INVALID = "登录账号需为4–32位字母、数字或下划线，以字母开头";

    public static final String INITIAL_PASSWORD_FORMAT_INVALID = "初始密码需包含字母和数字，长度10–64位";

    public static final String INITIAL_PASSWORD_BYTES_EXCEEDED = "初始密码的 UTF-8 长度不能超过72字节";

    public static final String ID_CARD_INVALID = "请输入正确的18位身份证号";

    public static final String ID_CARD_REQUIRED = "请输入身份证号";

    public static final String OPERATION_SAVE_FAILED = "账号操作记录保存失败";

    public static final String DISABLE_REASON_REQUIRED = "请填写停用原因";
}
