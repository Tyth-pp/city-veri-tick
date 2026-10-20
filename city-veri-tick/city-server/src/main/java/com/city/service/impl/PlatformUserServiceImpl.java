package com.city.service.impl;

import com.city.service.AccountOperationService;
import com.city.service.support.AccountOperationPolicy;

import com.city.mapper.UserMapper;
import com.city.entity.User;
import com.city.result.SystemResultCode;
import com.city.exception.BusinessException;
import com.city.security.TokenBlacklistService;
import com.city.security.RevocationReason;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.city.service.PlatformUserService;
import com.city.vo.PlatformUserViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class PlatformUserServiceImpl implements PlatformUserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private AccountOperationService accountOperations;
    private static final Logger log = LoggerFactory.getLogger(PlatformUserServiceImpl.class);

    private User manageable(Long actorId, Long userId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        if (actorId.equals(userId) || userMapper.countRole(userId, "PLATFORM_ADMIN") > 0
                || (userMapper.countRole(userId, "USER") == 0 && userMapper.countRole(userId, "ORGANIZER_ADMIN") == 0 && userMapper.countRole(userId, "VERIFIER") == 0)) {
            throw new BusinessException(SystemResultCode.FORBIDDEN);
        }
        return user;
    }

    @Override
    @Transactional
    public void updateEnabled(Long actorId, Long userId, boolean enabled) {
        updateEnabled(actorId, userId, enabled, null);
    }

    @Override
    @Transactional
    public void updateEnabled(Long actorId, Long userId, boolean enabled, String reason) {
        manageable(actorId, userId);
        String detail = AccountOperationPolicy.validate(reason, !enabled);
        // Redis failure must not commit a partial disable. Existing DB status is also checked at authentication.
        if (!enabled) tokenBlacklistService.revokeAll(userId, RevocationReason.ACCOUNT_DISABLED);
        User patch = new User();
        patch.setId(userId);
        patch.setEnabled(enabled);
        if (userMapper.updateById(patch) != 1) throw new BusinessException(SystemResultCode.CONFLICT);
        accountOperations.record(actorId, userId, enabled ? "ENABLE" : "DISABLE", detail);
        log.info("Account status changed: actor={}, target={}, enabled={}", actorId, userId, enabled);
    }

    @Override
    @Transactional
    public void forceLogout(Long actorId, Long userId) {
        forceLogout(actorId, userId, null);
    }

    @Override
    @Transactional
    public void forceLogout(Long actorId, Long userId, String reason) {
        User user = manageable(actorId, userId);
        String detail = AccountOperationPolicy.validate(reason, false);
        if (!Boolean.TRUE.equals(user.getEnabled())) throw new BusinessException(SystemResultCode.CONFLICT);
        tokenBlacklistService.revokeAll(userId, RevocationReason.FORCED_LOGOUT);
        accountOperations.record(actorId, userId, "FORCE_LOGOUT", detail);
        log.info("Account sessions ended: actor={}, target={}", actorId, userId);
    }

    @Override
    public List<PlatformUserViewVO> list(String keyword, String roleCode, Boolean enabled) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return userMapper.selectPlatformUsers().stream()
                .peek(item -> item.setOrganizers(userMapper.selectOrganizations(item.getId())))
                .filter(item -> enabled == null || enabled.equals(item.getEnabled()))
                .filter(item -> roleCode == null || roleCode.isBlank()
                        || (item.getRoleCodes() != null && item.getRoleCodes().contains(roleCode.trim())))
                .filter(item -> normalized.isBlank() || contains(item.getUsername(), normalized)
                        || contains(item.getNickname(), normalized) || contains(item.getName(), normalized) || contains(item.getPhone(), normalized)
                        || String.valueOf(item.getId()).contains(normalized))
                .toList();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
