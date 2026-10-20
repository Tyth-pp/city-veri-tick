package com.city.service.impl;

import com.city.constant.AccountMessageConstant;
import com.city.service.AccountOperationService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.entity.AccountOperation;
import com.city.entity.User;
import com.city.mapper.AccountOperationMapper;
import com.city.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountOperationServiceImpl implements AccountOperationService {
    @Autowired
    private AccountOperationMapper operations;
    @Autowired
    private UserMapper users;

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void record(Long actor, Long target, String action, String reason) {
        User user = users.selectById(actor);
        AccountOperation row = new AccountOperation();
        row.setOperatorId(actor);
        row.setOperatorName(user == null ? null : user.getStaffName());
        row.setOperatorUsername(user == null ? null : user.getUsername());
        row.setTargetUserId(target);
        row.setOperation(action);
        row.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        row.setCreatedAt(LocalDateTime.now());
        if (operations.insert(row) != 1) throw new IllegalStateException(AccountMessageConstant.OPERATION_SAVE_FAILED);
    }

    @Override
    public List<AccountOperation> list(Long target) {
        return operations.selectList(Wrappers.<AccountOperation>lambdaQuery().eq(AccountOperation::getTargetUserId, target).orderByDesc(AccountOperation::getCreatedAt).orderByDesc(AccountOperation::getId).last("LIMIT 100"));
    }
}
