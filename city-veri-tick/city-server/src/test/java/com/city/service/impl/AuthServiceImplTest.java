package com.city.service.impl;
import com.city.service.SmsCodeService;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import com.city.dto.SmsCodeRequest;
import com.city.dto.LoginDTO;
import com.city.result.SystemResultCode;
import com.city.exception.BusinessException;
import com.city.dto.SmsLoginDTO;
import com.city.entity.User;
import com.city.enums.UserRole;
import com.city.mapper.UserMapper;
import com.city.security.JwtTokenService;
import com.city.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.time.Instant;
import com.city.security.LoginPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {
    private static final String PHONE = "13800138000";
    private final AuthServiceImpl service = new AuthServiceImpl();
    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final SmsCodeService smsCodeService = mock(SmsCodeService.class);
    private final TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "smsCodeService", smsCodeService);
        when(smsCodeService.reserve(PHONE, "123456")).thenReturn("lease");
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "jwtTokenService", jwtTokenService);
        ReflectionTestUtils.setField(service, "tokenBlacklistService", tokenBlacklistService);
        when(jwtTokenService.createToken(any(), eq("USER"))).thenReturn("token");
        when(jwtTokenService.parse("token")).thenReturn(Optional.empty());
    }

    @Test
    void wrongCodeShowsSpecificMessageWithoutConsumingValidCode() {
        SmsLoginDTO request = issuedCode();
        request.setCode("000000");
        when(smsCodeService.reserve(PHONE,"000000")).thenThrow(new BusinessException(SystemResultCode.SMS_CODE_INVALID));
        assertThatThrownBy(() -> service.loginBySms(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(SystemResultCode.SMS_CODE_INVALID.message());
        verify(smsCodeService, never()).complete(anyString(), anyString());
        verifyNoInteractions(userMapper);
    }

    @Test
    void unknownUsernameAndWrongPasswordShowSameMessage() {
        LoginDTO request = new LoginDTO();
        request.setUsername("staff");
        request.setPassword("wrong-password");
        assertThatThrownBy(() -> service.login(request)).hasMessage("用户名或密码输入错误");
        User user = new User();
        user.setId(7L);
        user.setEnabled(true);
        user.setPasswordHash("hash");
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user);
        when(userMapper.selectByUsernameWithRole("staff")).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);
        assertThatThrownBy(() -> service.login(request)).hasMessage("用户名或密码输入错误");
    }

    @Test
    void createsUserAndRoleOnFirstSmsLogin() {
        when(userMapper.selectRoleId("USER")).thenReturn(4L);
        when(passwordEncoder.encode(any())).thenReturn("hashed-password");
        doAnswer(invocation -> {
            User created = invocation.getArgument(0);
            created.setId(42L);
            when(userMapper.selectByIdForUpdate(42L)).thenReturn(created);
            return 1;
        }).when(userMapper).insert(any(User.class));

        SmsLoginDTO request = issuedCode();
        assertThat(service.loginBySms(request).getRole()).isEqualTo("USER");
        verify(userMapper).insertRole(42L, 4L);
        verify(userMapper).insert(argThat((User user) -> user.getNickname() != null
                && !user.getNickname().equals("Venuo用户") && user.getNickname().length() >= 4));
        verify(smsCodeService).complete(PHONE,"lease");
    }

    @Test
    void repairsExistingUserWithoutRole() {
        User user = new User();
        user.setId(42L);
        user.setEnabled(true);
        user.setUsername("user_" + PHONE);
        when(userMapper.selectByPhone(PHONE)).thenReturn(user);
        when(userMapper.selectRoleId("USER")).thenReturn(4L);
        when(userMapper.selectByIdForUpdate(42L)).thenReturn(user);

        assertThat(service.loginBySms(issuedCode()).getRole()).isEqualTo("USER");
        verify(userMapper, never()).insert(any(User.class));
        verify(userMapper).insertRole(42L, 4L);
    }

    @Test
    void keepsCodeWhenRoleBindingFailsSoLoginCanBeRetried() {
        User user = new User();
        user.setId(42L);
        user.setEnabled(true);
        user.setUsername("user_" + PHONE);
        when(userMapper.selectByPhone(PHONE)).thenReturn(user);
        when(userMapper.selectRoleId("USER")).thenReturn(4L);
        when(userMapper.selectByIdForUpdate(42L)).thenReturn(user);
        when(userMapper.insertRole(42L, 4L)).thenThrow(new IllegalStateException("database unavailable")).thenReturn(1);
        SmsLoginDTO request = issuedCode();

        assertThatThrownBy(() -> service.loginBySms(request)).isInstanceOf(IllegalStateException.class);
        verify(smsCodeService).releaseQuietly(PHONE,"lease");
        assertThat(service.loginBySms(request).getRole()).isEqualTo("USER");
    }

    @Test
    void rejectsPrivilegedAccountThroughSmsLogin() {
        User user = new User();
        user.setId(7L);
        user.setEnabled(true);
        user.setRole(UserRole.PLATFORM_ADMIN);
        when(userMapper.selectByPhoneWithRole(PHONE)).thenReturn(user);

        assertThatThrownBy(() -> service.loginBySms(issuedCode())).isInstanceOf(RuntimeException.class);
        verify(userMapper, never()).insertRole(any(), any());
    }

    @Test
    void doesNotGrantUserRoleToUnrelatedAccountWithoutRole() {
        User user = new User();
        user.setId(7L);
        user.setEnabled(true);
        user.setUsername("unassigned_staff");
        when(userMapper.selectByPhone(PHONE)).thenReturn(user);

        assertThatThrownBy(() -> service.loginBySms(issuedCode())).isInstanceOf(RuntimeException.class);
        verify(userMapper, never()).insertRole(any(), any());
    }

    private SmsLoginDTO issuedCode() {
        SmsLoginDTO login = new SmsLoginDTO();
        login.setPhone(PHONE);
        login.setCode("123456");
        return login;
    }

    @Test void disabledPasswordAccountGetsSpecificFeedbackAndNoToken() {
        User user=new User(); user.setId(7L); user.setEnabled(false); user.setPasswordHash("hash");
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user);
        when(userMapper.selectByUsernameWithRole("staff")).thenReturn(user);
        when(passwordEncoder.matches("correct","hash")).thenReturn(true);
        LoginDTO request=new LoginDTO(); request.setUsername("staff"); request.setPassword("correct");
        assertThatThrownBy(() -> service.login(request)).hasMessage(SystemResultCode.ACCOUNT_DISABLED.message());
        verify(jwtTokenService,never()).createToken(any(),any());
    }
    @Test void disabledSmsAccountCannotLoginOrObtainANewToken() {
        User user=new User(); user.setId(7L); user.setEnabled(false); user.setRole(UserRole.USER);
        when(userMapper.selectByPhoneWithRole(PHONE)).thenReturn(user);
        assertThatThrownBy(() -> service.loginBySms(issuedCode())).hasMessage(SystemResultCode.ACCOUNT_DISABLED.message());
        verify(jwtTokenService,never()).createToken(any(),any());
    }

    @Test
    void logoutBlacklistsTokenAndRemovesItsSessionReference() {
        Instant expiry = Instant.now().plusSeconds(300);
        when(jwtTokenService.parse("token")).thenReturn(Optional.of(
                new LoginPrincipal(7L, "USER", "jti-7", Instant.now(), expiry)));

        service.logout("token");

        verify(tokenBlacklistService).revoke(eq("jti-7"), eq(expiry));
        verify(tokenBlacklistService).remove(7L, "jti-7");
    }

    @Test void delegatesSendingAndReturnsActualCooldown() {
        when(smsCodeService.send(PHONE)).thenReturn(60);
        SmsCodeRequest request=new SmsCodeRequest(); request.setPhone(PHONE);
        service.sendSmsCode(request);
        verify(smsCodeService).send(PHONE);
    }
    @Test void redisVerificationFailureNeverIssuesAJwtOrTouchesAccounts() {
        when(smsCodeService.reserve(PHONE,"123456")).thenThrow(new DataAccessResourceFailureException("unavailable"));
        assertThatThrownBy(()->service.loginBySms(issuedCode())).isInstanceOf(DataAccessException.class);
        verifyNoInteractions(userMapper,tokenBlacklistService);
        verify(jwtTokenService,never()).createToken(any(),any());
    }
    @Test void registersJwtBeforeConsumingTheReservedCode() {
        User user=new User(); user.setId(7L); user.setEnabled(true); user.setRole(UserRole.USER);
        when(userMapper.selectByPhoneWithRole(PHONE)).thenReturn(user);
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user);
        Instant expiry=Instant.now().plusSeconds(300);
        when(jwtTokenService.parse("token")).thenReturn(Optional.of(new LoginPrincipal(7L,"USER","new-jti",Instant.now(),expiry)));
        service.loginBySms(issuedCode());
        var sequence=inOrder(tokenBlacklistService,smsCodeService);
        sequence.verify(tokenBlacklistService).register(7L,"new-jti",expiry);
        sequence.verify(smsCodeService).complete(PHONE,"lease");
    }
    @Test void failedCodeConsumptionNeverReturnsLoginSuccess() {
        User user=new User(); user.setId(7L); user.setEnabled(true); user.setRole(UserRole.USER);
        when(userMapper.selectByPhoneWithRole(PHONE)).thenReturn(user);
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user);
        doThrow(new DataAccessResourceFailureException("unconfirmed")).when(smsCodeService).complete(PHONE,"lease");
        assertThatThrownBy(()->service.loginBySms(issuedCode())).isInstanceOf(DataAccessException.class);
        verify(smsCodeService).releaseQuietly(PHONE,"lease");
    }
}
