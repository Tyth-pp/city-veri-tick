package com.city.security;

import com.city.entity.User;
import com.city.mapper.UserMapper;
import org.springframework.stereotype.Component;

/**
 * 数据库账户状态检查器实现类
 * 该类实现了AccountStatusChecker接口，用于检查用户账户状态
 */
@Component
public class DatabaseAccountStatusChecker implements AccountStatusChecker {
    /**
     * 检查指定用户是否需要修改密码
     * @param userId 用户ID
     * @return 如果用户存在且需要修改密码则返回true，否则返回false
     */
    @Override
    public boolean mustChangePassword(Long userId) {
        var user = mapper.selectById(userId);
        return user != null && Boolean.TRUE.equals(user.getMustChangePassword());
    }

    /**
     * 用户数据访问映射器
     * 用于与数据库进行交互，执行用户相关的数据操作
     */
    private final UserMapper mapper;

    /**
     * 构造函数，通过依赖注入方式获取UserMapper实例
     * @param mapper 用户数据访问映射器
     */
    public DatabaseAccountStatusChecker(UserMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 检查指定用户账户是否启用
     * @param userId 用户ID
     * @return 如果用户存在且账户已启用则返回true，否则返回false
     */
    @Override
    public boolean isEnabled(Long userId) {
        User user = mapper.selectById(userId);
        return user != null && Boolean.TRUE.equals(user.getEnabled());
    }
}
