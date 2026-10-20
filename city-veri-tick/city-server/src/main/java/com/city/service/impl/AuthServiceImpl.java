package com.city.service.impl;
import com.city.service.SmsCodeService;

import com.city.enums.UserRole;

import com.city.result.SystemResultCode;
import com.city.dto.LoginDTO;
import com.city.dto.SmsCodeRequest;
import com.city.dto.SmsLoginDTO;
import com.city.entity.User;
import com.city.exception.BusinessException;
import com.city.mapper.UserMapper;
import com.city.security.JwtTokenService;
import com.city.security.TokenBlacklistService;
import com.city.security.RevocationReason;
import com.city.service.AuthService;
import com.city.vo.LoginVO;
import com.city.vo.SmsCodeVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String[] NICKNAME_ADJECTIVES = {"神秘的", "勇敢的", "自在的", "慷慨的", "闪亮的", "好奇的", "温柔的", "热情的"};
    private static final String[] NICKNAME_NOUNS = {"幽灵", "红参", "星河", "旅人", "飞鸟", "晚风", "青松", "海浪"};
    @Autowired
    private SmsCodeService smsCodeService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    public SmsCodeVO sendSmsCode(SmsCodeRequest request) {
        return new SmsCodeVO(smsCodeService.send(request.getPhone()));
    }

    @Override
    @Transactional
    public LoginVO loginBySms(SmsLoginDTO request) {
        String phone = request.getPhone();
        String lease = smsCodeService.reserve(phone, request.getCode());
        boolean completed = false;
        try {
            User user = userMapper.selectByPhoneWithRole(phone);
            if (user == null) {
                user = userMapper.selectByPhone(phone);
                if (user == null) {
                    user = new User();
                    user.setUsername("user_" + phone);
                    user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    user.setPhone(phone);
                    user.setNickname(randomNickname());
                    user.setEnabled(true);
                    userMapper.insert(user);
                } else if (!("user_" + phone).equals(user.getUsername())) {
                    throw new BusinessException(SystemResultCode.UNAUTHORIZED);
                }
                if (!Boolean.TRUE.equals(user.getEnabled())) {
                    throw new BusinessException(SystemResultCode.ACCOUNT_DISABLED);
                }
                Long roleId = userMapper.selectRoleId("USER");
                if (roleId == null) throw new BusinessException(SystemResultCode.INTERNAL_ERROR);
                userMapper.insertRole(user.getId(), roleId);
                user.setRole(UserRole.USER);
            }
            if (!Boolean.TRUE.equals(user.getEnabled())) throw new BusinessException(SystemResultCode.ACCOUNT_DISABLED);
            if (user.getRole() != UserRole.USER) {
                throw new BusinessException(SystemResultCode.UNAUTHORIZED);
            }
            user = lockAccount(user);
            if (!Boolean.TRUE.equals(user.getEnabled())) throw new BusinessException(SystemResultCode.ACCOUNT_DISABLED);
            LoginVO login = issueToken(user);
            smsCodeService.complete(phone, lease);
            completed = true;
            return login;
        } finally {
            if (!completed) smsCodeService.releaseQuietly(phone, lease);
        }
    }

    @Override
    @Transactional
    public LoginVO login(LoginDTO loginDTO) {
        User user = userMapper.selectByUsernameWithRole(loginDTO.getUsername());
        if (user != null) user = lockAccount(user);
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(SystemResultCode.LOGIN_CREDENTIALS_INVALID);
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) throw new BusinessException(SystemResultCode.ACCOUNT_DISABLED);
        return issueToken(user);
    }

    private User lockAccount(User snapshot) {
        User current = userMapper.selectByIdForUpdate(snapshot.getId());
        if (current == null) throw new BusinessException(SystemResultCode.LOGIN_CREDENTIALS_INVALID);
        current.setRole(snapshot.getRole());
        return current;
    }

    private LoginVO issueToken(User user) {
        String token = Boolean.TRUE.equals(user.getMustChangePassword())
                ? jwtTokenService.createInitialPasswordToken(user.getId(), user.getRole().name())
                : jwtTokenService.createToken(user.getId(), user.getRole().name());
        jwtTokenService.parse(token).ifPresent(principal -> tokenBlacklistService.register(principal.userId(), principal.jti(),
                principal.expiresAt()));
        LoginVO login = new LoginVO(token, user.getRole().name());
        login.setMustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()));
        return login;
    }

    static String randomNickname() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return NICKNAME_ADJECTIVES[random.nextInt(NICKNAME_ADJECTIVES.length)]
                + NICKNAME_NOUNS[random.nextInt(NICKNAME_NOUNS.length)];
    }

    @Override
    public void logout(String token) {
        jwtTokenService.parse(token).ifPresent(principal -> {
            tokenBlacklistService.revoke(principal.jti(), principal.expiresAt());
            tokenBlacklistService.remove(principal.userId(), principal.jti());
        });
    }

    @Override
    @Transactional
    public void forceLogout(Long userId) {
        if (userMapper.selectByIdForUpdate(userId) == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        tokenBlacklistService.revokeAll(userId, RevocationReason.FORCED_LOGOUT);
    }
}
