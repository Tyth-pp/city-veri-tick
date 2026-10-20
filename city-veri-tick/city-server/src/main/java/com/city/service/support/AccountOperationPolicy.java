package com.city.service.support;

import com.city.constant.AccountMessageConstant;
import com.city.exception.AccountOperationException;

/**
 * 账户操作策略类，用于验证账户操作的原因描述
 * 该类采用单例模式设计，通过私有构造函数确保不能被实例化
 */
public final class AccountOperationPolicy {
    // 私有构造函数，防止该类被实例化
    private AccountOperationPolicy() { }

    /**
     * 验证账户操作原因的方法
     * @param reason 原因描述字符串
     * @param required 是否为必填项
     * @return 验证后的原因描述字符串，如果为null则返回空字符串
     * @throws AccountOperationException 当原因不符合要求时抛出异常
     */
    public static String validate(String reason, boolean required) {
        // 处理null值，并将其转换为空字符串，同时去除前后空格
        String value = reason == null ? "" : reason.trim();
        // 如果是必填项且值为空，抛出异常
        if (required && value.isEmpty()) throw new AccountOperationException(AccountMessageConstant.DISABLE_REASON_REQUIRED);
        // 如果原因长度超过500字，抛出异常
        if (value.length() > 500) throw new AccountOperationException(AccountMessageConstant.OPERATION_REASON_TOO_LONG);
        return value;
    }
}
