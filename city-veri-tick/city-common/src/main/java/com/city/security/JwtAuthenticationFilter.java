package com.city.security;

import com.city.constant.AccountMessageConstant;
import com.city.constant.AuthMessageConstant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private AccountStatusChecker accountStatusChecker;
    @Autowired
    private AuthenticationFailureResponse failureResponse;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/controller/auth/") || request.getRequestURI().startsWith("/controller/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        var parsed = jwtTokenService.parse(header.substring(7));
        if (parsed.isEmpty()) {
            reject(response, 40101, AuthMessageConstant.LOGIN_EXPIRED);
            return;
        }
        var principal = parsed.get();
        try {
            if (!accountStatusChecker.isEnabled(principal.userId())) {
                reject(response, 40102, AccountMessageConstant.ACCOUNT_DISABLED);
                return;
            }
            RevocationReason reason = tokenBlacklistService.reason(principal.jti());
            if (reason != null) {
                if (reason == RevocationReason.ACCOUNT_DISABLED)
                    reject(response, 40104, AuthMessageConstant.SESSION_ACCOUNT_DISABLED);
                else if (reason == RevocationReason.FORCED_LOGOUT)
                    reject(response, 40103, AuthMessageConstant.FORCED_LOGOUT);
                else reject(response, 40104, AuthMessageConstant.SESSION_ENDED);
                return;
            }
            String path = request.getRequestURI();
            boolean mustChange = accountStatusChecker.mustChangePassword(principal.userId());
            if (principal.initialPasswordOnly() && !mustChange) {
                reject(response, 40104, AuthMessageConstant.INITIAL_PASSWORD_CHANGED);
                return;
            }
            if ((mustChange || principal.initialPasswordOnly())
                    && !(path.equals("/controller/account/password") && request.getMethod().equals("POST"))
                    && !(path.equals("/controller/account/session") && request.getMethod().equals("GET"))) {
                SecurityContextHolder.clearContext();
                failureResponse.write(response, 403, 40301, AuthMessageConstant.INITIAL_PASSWORD_CHANGE_REQUIRED);
                return;
            }
        } catch (DataAccessException e) {
            SecurityContextHolder.clearContext();
            failureResponse.write(response, 503, 50301, AuthMessageConstant.SESSION_CHECK_UNAVAILABLE);
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int code, String message) throws IOException {
        SecurityContextHolder.clearContext();
        failureResponse.write(response, 401, code, message);
    }
}
