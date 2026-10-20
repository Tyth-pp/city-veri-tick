package com.city.security;

/**
 * RevocationReason 枚举类，用于表示凭证撤销的原因
 * 包含多种预定义的撤销原因类型，并提供一个静态方法用于从字符串值获取对应的枚举实例
 */
public enum RevocationReason {
    LOGOUT, FORCED_LOGOUT, ACCOUNT_DISABLED, LEGACY;

    public static RevocationReason fromValue(String value) {
        if (value == null) return null;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return LEGACY;
        }
    }
}
