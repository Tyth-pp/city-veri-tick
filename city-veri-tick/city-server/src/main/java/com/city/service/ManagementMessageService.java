package com.city.service;

import com.city.entity.Activity;
import com.city.security.LoginPrincipal;

import java.util.Map;

/**
 * 管理端业务消息及已读管理的业务契约。
 */
public interface ManagementMessageService {
    Map<String, Object> inbox(LoginPrincipal user, int page);

    void read(LoginPrincipal user, Long id);

    void submission(Activity activity, Long userId);

    void publish(String audience, Long organizerId, String type, String title, String content, String path);
}
