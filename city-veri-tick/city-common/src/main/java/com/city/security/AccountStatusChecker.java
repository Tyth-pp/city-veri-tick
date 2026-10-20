package com.city.security;

/**
 * 账户状态检查器接口
 * 该接口用于检查用户账户的状态信息
 */
public interface AccountStatusChecker {
    /**
     * 检查指定用户ID的账户是否启用
     * @param userId 用户ID
     * @return 如果账户启用返回true，否则返回false
     */
    boolean isEnabled(Long userId);

    /**
     * 检查指定用户ID的账户是否需要修改密码
     * 这是一个默认方法，子类可以选择重写
     * @param userId 用户ID
     * @return 如果需要修改密码返回true，否则返回false
     */
    default boolean mustChangePassword(Long userId) {
        return false;
    }
}
