package com.city.service.impl;
import com.city.service.AccountOperationService;

import com.city.entity.User;
import com.city.mapper.UserMapper;
import com.city.security.TokenBlacklistService;
import com.city.security.RevocationReason;
import com.city.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlatformAccountStatusTest {
    private final PlatformUserServiceImpl service = new PlatformUserServiceImpl();
    private final UserMapper mapper = mock(UserMapper.class);
    private final TokenBlacklistService tokens = mock(TokenBlacklistService.class);
    private final AccountOperationService operations=mock(AccountOperationService.class);
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service, "userMapper", mapper);
        ReflectionTestUtils.setField(service, "tokenBlacklistService", tokens);
        ReflectionTestUtils.setField(service,"accountOperations",operations);
        User user = new User(); user.setId(7L); user.setEnabled(true);
        when(mapper.selectByIdForUpdate(7L)).thenReturn(user);
        when(mapper.countRole(7L, "USER")).thenReturn(1L);
        when(mapper.updateById(any(User.class))).thenReturn(1);
    }
    @Test void disablingRevokesSessionsAndPersistsStatus() {
        service.updateEnabled(1L, 7L, false,"违规资料需核实");
        verify(operations).record(1L,7L,"DISABLE","违规资料需核实");
        verify(tokens).revokeAll(7L, RevocationReason.ACCOUNT_DISABLED);
        verify(mapper).updateById(argThat((User u) -> u.getId().equals(7L) && !u.getEnabled()));
    }
    @Test void disablingRequiresReasonBeforeRevokingAnyToken() {
        assertThatThrownBy(() -> service.updateEnabled(1L,7L,false)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(tokens);
        verify(mapper,never()).updateById(any(User.class));
    }
    @Test void organizerAdministratorUsesTheSameStatusRules() {
        when(mapper.countRole(7L,"USER")).thenReturn(0L);
        when(mapper.countRole(7L,"ORGANIZER_ADMIN")).thenReturn(1L);
        service.updateEnabled(1L,7L,false,"人员离职");
        verify(tokens).revokeAll(7L,RevocationReason.ACCOUNT_DISABLED);
    }
    @Test void enablingDoesNotRestoreOldTokens() {
        service.updateEnabled(1L,7L,true);
        verifyNoInteractions(tokens);
        verify(operations).record(1L,7L,"ENABLE","");
    }
    @Test void forceLogoutKeepsAccountEnabledAndRecordsActualActor(){
        service.forceLogout(1L,7L,"账号使用情况需核实");
        verify(tokens).revokeAll(7L,RevocationReason.FORCED_LOGOUT);
        verify(operations).record(1L,7L,"FORCE_LOGOUT","账号使用情况需核实");
        verify(mapper,never()).updateById(any(User.class));
    }
    @Test void platformAccountsAndSelfDisableAreProtected() {
        when(mapper.countRole(7L,"PLATFORM_ADMIN")).thenReturn(1L);
        assertThatThrownBy(() -> service.updateEnabled(1L,7L,false)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.updateEnabled(7L,7L,false)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(tokens);
        verify(mapper,never()).updateById(any(User.class));
    }
    @Test void revocationFailureDoesNotPersistAnIncompleteDisable() {
        doThrow(new IllegalStateException("redis unavailable")).when(tokens).revokeAll(7L,RevocationReason.ACCOUNT_DISABLED);
        assertThatThrownBy(() -> service.updateEnabled(1L,7L,false,"核实信息")).isInstanceOf(IllegalStateException.class);
        verify(mapper,never()).updateById(any(User.class));
        verifyNoInteractions(operations);
    }
}
