package com.city.security;
import org.springframework.dao.DataAccessResourceFailureException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionRejectionTest {
    @Test void initialPasswordTokenNeverBecomesARegularSessionAfterPasswordChange() throws Exception {
        JwtTokenService jwt=mock(JwtTokenService.class);TokenBlacklistService tokens=mock(TokenBlacklistService.class);
        AccountStatusChecker accounts=mock(AccountStatusChecker.class);JwtAuthenticationFilter filter=new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter,"jwtTokenService",jwt);ReflectionTestUtils.setField(filter,"tokenBlacklistService",tokens);
        ReflectionTestUtils.setField(filter,"accountStatusChecker",accounts);ReflectionTestUtils.setField(filter,"failureResponse",new AuthenticationFailureResponse(new ObjectMapper()));
        when(jwt.parse("token")).thenReturn(Optional.of(new LoginPrincipal(7L,"VERIFIER","jti",Instant.now(),Instant.now().plusSeconds(60),true)));
        when(accounts.isEnabled(7L)).thenReturn(true);when(accounts.mustChangePassword(7L)).thenReturn(false);
        var request=new MockHttpServletRequest("GET","/controller/verify/tickets/records");request.addHeader("Authorization","Bearer token");
        var response=new MockHttpServletResponse();
        filter.doFilter(request,response,(a,b)->{throw new AssertionError("setup token cannot become business session");});
        assertThat(response.getStatus()).isEqualTo(401);assertThat(response.getContentAsString()).contains("40104");
    }
    @Test void initialPasswordSessionOnlyAllowsSessionAndPasswordEndpoints() throws Exception {
        JwtTokenService jwt=mock(JwtTokenService.class);TokenBlacklistService tokens=mock(TokenBlacklistService.class);
        AccountStatusChecker accounts=mock(AccountStatusChecker.class);JwtAuthenticationFilter filter=new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter,"jwtTokenService",jwt);ReflectionTestUtils.setField(filter,"tokenBlacklistService",tokens);
        ReflectionTestUtils.setField(filter,"accountStatusChecker",accounts);ReflectionTestUtils.setField(filter,"failureResponse",new AuthenticationFailureResponse(new ObjectMapper()));
        when(jwt.parse("token")).thenReturn(Optional.of(new LoginPrincipal(7L,"VERIFIER","jti",Instant.now(),Instant.now().plusSeconds(60))));
        when(accounts.isEnabled(7L)).thenReturn(true);when(accounts.mustChangePassword(7L)).thenReturn(true);
        var request=new MockHttpServletRequest("POST","/controller/verify/tickets/check-in");request.addHeader("Authorization","Bearer token");
        var response=new MockHttpServletResponse();
        filter.doFilter(request,response,(a,b)->{throw new AssertionError("initial password cannot authorize business");});
        assertThat(response.getStatus()).isEqualTo(403);assertThat(response.getContentAsString()).contains("40301");
        try {
            for (String path:new String[]{"/controller/account/session","/controller/account/password"}) {
                var allowed=new MockHttpServletRequest(path.endsWith("session")?"GET":"POST",path);allowed.addHeader("Authorization","Bearer token");
                filter.doFilter(allowed,new MockHttpServletResponse(),(a,b)->assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull());
            }
        } finally {SecurityContextHolder.clearContext();}
    }
    @Test void disabledAccountAndForcedLogoutReturnDifferentReasonsAndDoNotExecuteBusiness() throws Exception {
        JwtTokenService jwt=mock(JwtTokenService.class);
        TokenBlacklistService tokens=mock(TokenBlacklistService.class);
        AccountStatusChecker accounts=mock(AccountStatusChecker.class);
        JwtAuthenticationFilter filter=new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter,"jwtTokenService",jwt);
        ReflectionTestUtils.setField(filter,"tokenBlacklistService",tokens);
        ReflectionTestUtils.setField(filter,"accountStatusChecker",accounts);
        ReflectionTestUtils.setField(filter,"failureResponse",new AuthenticationFailureResponse(new ObjectMapper()));
        when(jwt.parse("token")).thenReturn(Optional.of(new LoginPrincipal(7L,"USER","abc",Instant.now(),Instant.now().plusSeconds(60))));
        for (boolean enabled : new boolean[]{false,true}) {
            SecurityContextHolder.clearContext();
            when(accounts.isEnabled(7L)).thenReturn(enabled);
            when(tokens.reason("abc")).thenReturn(RevocationReason.FORCED_LOGOUT);
            var request=new MockHttpServletRequest("GET","/controller/user/orders"); request.addHeader("Authorization","Bearer token");
            var response=new MockHttpServletResponse();
            filter.doFilter(request,response,(a,b)->{throw new AssertionError("business must not run");});
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).contains(enabled ? "40103" : "40102");
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Test void reenabledAccountRejectsOldSessionButAcceptsNewLogin() throws Exception {
        JwtTokenService jwt=mock(JwtTokenService.class);
        TokenBlacklistService tokens=mock(TokenBlacklistService.class);
        AccountStatusChecker accounts=mock(AccountStatusChecker.class);
        JwtAuthenticationFilter filter=new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter,"jwtTokenService",jwt);
        ReflectionTestUtils.setField(filter,"tokenBlacklistService",tokens);
        ReflectionTestUtils.setField(filter,"accountStatusChecker",accounts);
        ReflectionTestUtils.setField(filter,"failureResponse",new AuthenticationFailureResponse(new ObjectMapper()));
        when(accounts.isEnabled(7L)).thenReturn(true);
        when(jwt.parse("token")).thenReturn(Optional.of(new LoginPrincipal(7L,"USER","abc",Instant.now(),Instant.now().plusSeconds(60))));
        when(jwt.parse("new-token")).thenReturn(Optional.of(new LoginPrincipal(7L,"USER","new-jti",Instant.now(),Instant.now().plusSeconds(60))));
        when(tokens.reason("abc")).thenReturn(RevocationReason.ACCOUNT_DISABLED);
        var request=new MockHttpServletRequest("GET","/controller/user/orders");
        request.addHeader("Authorization","Bearer token");
        var oldResponse=new MockHttpServletResponse();
        filter.doFilter(request,oldResponse,(a,b)->{throw new AssertionError("old session must not run business");});
        assertThat(oldResponse.getStatus()).isEqualTo(401);
        assertThat(oldResponse.getContentAsString()).contains("40104");
        var newResponse=new MockHttpServletResponse();
        var newRequest=new MockHttpServletRequest("GET","/controller/user/orders");
        newRequest.addHeader("Authorization","Bearer new-token");
        try {
            filter.doFilter(newRequest,newResponse,(a,b)->assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull());
            assertThat(newResponse.getStatus()).isEqualTo(200);
        } finally { SecurityContextHolder.clearContext(); }
    }
    @Test void redisUnavailableReturns503AndNeverRunsAuthenticatedBusiness() throws Exception {
        JwtTokenService jwt=mock(JwtTokenService.class);
        TokenBlacklistService tokens=mock(TokenBlacklistService.class);
        AccountStatusChecker accounts=mock(AccountStatusChecker.class);
        JwtAuthenticationFilter filter=new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter,"jwtTokenService",jwt);
        ReflectionTestUtils.setField(filter,"tokenBlacklistService",tokens);
        ReflectionTestUtils.setField(filter,"accountStatusChecker",accounts);
        ReflectionTestUtils.setField(filter,"failureResponse",new AuthenticationFailureResponse(new ObjectMapper()));
        when(jwt.parse("token")).thenReturn(Optional.of(new LoginPrincipal(7L,"USER","abc",Instant.now(),Instant.now().plusSeconds(60))));
        when(accounts.isEnabled(7L)).thenReturn(true);
        when(tokens.reason("abc")).thenThrow(new DataAccessResourceFailureException("unavailable"));
        var request=new MockHttpServletRequest("GET","/controller/user/orders");
        request.addHeader("Authorization","Bearer token");
        var response=new MockHttpServletResponse();
        try {
            filter.doFilter(request,response,(a,b)->{throw new AssertionError("unverified session must not reach business");});
            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(response.getContentAsString()).contains("50301");
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        } finally { SecurityContextHolder.clearContext(); }
    }
}
