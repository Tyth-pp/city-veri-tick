package com.city.service;

import com.city.entity.AccountOperation;

import java.util.List;

/**
 * 账号操作审计记录的业务契约。
 */
public interface AccountOperationService {
    void record(Long actor, Long target, String action, String reason);

    List<AccountOperation> list(Long target);
}
