package com.city.service.impl;
import com.city.service.AccountOperationService;
import com.city.dto.LoginDTO;
import com.city.entity.User;
import com.city.enums.UserRole;
import com.city.mapper.UserMapper;
import com.city.security.JwtTokenService;
import com.city.security.TokenBlacklistService;
import com.city.security.RevocationReason;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AccountLifecycleLockTest {
    @Test void loginRechecksStatusAndCredentialsFromLockedRowBeforeIssuingToken() {
        var service=new AuthServiceImpl();var users=mock(UserMapper.class);var jwt=mock(JwtTokenService.class);
        var encoder=mock(PasswordEncoder.class);
        ReflectionTestUtils.setField(service,"userMapper",users);ReflectionTestUtils.setField(service,"jwtTokenService",jwt);
        ReflectionTestUtils.setField(service,"passwordEncoder",encoder);
        User snapshot=new User();snapshot.setId(7L);snapshot.setRole(UserRole.VERIFIER);snapshot.setEnabled(true);snapshot.setPasswordHash("old");
        when(users.selectByUsernameWithRole("verify_one")).thenReturn(snapshot);
        User locked=new User();locked.setId(7L);locked.setEnabled(false);locked.setPasswordHash("new");
        when(users.selectByIdForUpdate(7L)).thenReturn(locked);
        LoginDTO d=new LoginDTO();d.setUsername("verify_one");d.setPassword("Initial123!");
        assertThatThrownBy(()->service.login(d)).hasMessage("用户名或密码输入错误");
        verify(encoder).matches("Initial123!","new");verifyNoInteractions(jwt);
        when(encoder.matches("Initial123!","new")).thenReturn(true);
        assertThatThrownBy(()->service.login(d)).hasMessage("账号已停用，暂时无法使用，请联系平台管理员。");
        verifyNoInteractions(jwt);
    }
    @Test void disablingLocksAccountBeforeRedisRevocationAndStatusMutation() {
        var service=new PlatformUserServiceImpl();var users=mock(UserMapper.class);var tokens=mock(TokenBlacklistService.class);
        ReflectionTestUtils.setField(service,"userMapper",users);ReflectionTestUtils.setField(service,"tokenBlacklistService",tokens);
        ReflectionTestUtils.setField(service,"accountOperations",mock(AccountOperationService.class));
        User u=new User();u.setId(7L);u.setEnabled(true);when(users.selectByIdForUpdate(7L)).thenReturn(u);
        when(users.countRole(7L,"USER")).thenReturn(1L);when(users.updateById(any(User.class))).thenReturn(1);
        service.updateEnabled(1L,7L,false,"核实账号信息");
        var order=inOrder(users,tokens);order.verify(users).selectByIdForUpdate(7L);
        order.verify(tokens).revokeAll(7L,RevocationReason.ACCOUNT_DISABLED);
        order.verify(users).updateById(any(User.class));
    }
}
