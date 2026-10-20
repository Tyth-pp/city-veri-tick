package com.city.security;

import java.time.Instant;

/**
 * 登录主体记录类，用于封装用户登录信息
 * record类型是一个不可变的数据载体，适用于存储和传递数据
 */
public record LoginPrincipal(Long userId, String role, String jti, Instant issuedAt, Instant expiresAt, boolean initialPasswordOnly) {
    /**
     * 登录主体的构造函数，提供默认的initialPasswordOnly参数为false
     * @param userId 用户ID
     * @param role 用户角色
     * @param jti JSON令牌标识符，用于唯一标识一个令牌
     * @param issuedAt 令牌签发时间
     * @param expiresAt 令牌过期时间
     */
    public LoginPrincipal(Long userId,String role,String jti,Instant issuedAt,Instant expiresAt) {
        // 调用全参构造函数，将initialPasswordOnly默认设置为false
        this(userId,role,jti,issuedAt,expiresAt,false);
    }
}
