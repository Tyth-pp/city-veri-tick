package com.city.config;

import com.city.constant.AuthMessageConstant;
import com.city.security.AuthenticationFailureResponse;
import com.city.security.JwtAuthenticationFilter;
import com.city.security.JwtTokenService;
import com.city.properties.JwtProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import jakarta.servlet.DispatcherType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 安全配置类，用于配置系统的安全策略，包括JWT认证、密码加密、访问控制等
 * 该类使用了Spring Security框架，并启用了定时任务功能
 */
@Configuration
@EnableWebSecurity
@EnableScheduling
public class SecurityConfig {
    /**
     * 创建Clock Bean，用于获取指定时区的当前时间
     * @return 返回一个使用"Asia/Shanghai"时区的Clock实例
     */
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }

    /**
     * 创建JWT Token服务Bean
     * @param properties JWT配置属性
     * @param clock 时钟对象，用于获取当前时间
     * @return 返回一个JwtTokenService实例
     */
    @Bean
    JwtTokenService jwtTokenService(JwtProperties properties, Clock clock) {
        return new JwtTokenService(properties, clock);
    }

    /**
     * 配置JWT认证过滤器注册
     * 注意：这里将过滤器禁用，因为我们会通过其他方式手动添加过滤器
     * @param filter JWT认证过滤器实例
     * @return 返回一个配置好的FilterRegistrationBean
     */
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 创建密码编码器Bean
     * 使用BCrypt算法进行密码加密，这是一种安全的哈希算法
     * @return 返回一个BCryptPasswordEncoder实例
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置安全过滤器链，定义了系统的安全策略
     * @param http HttpSecurity对象，用于配置安全策略
     * @param jwtAuthenticationFilter JWT认证过滤器
     * @param failures 认证失败处理器
     * @return 返回配置好的SecurityFilterChain
     * @throws Exception 可能抛出的安全配置异常
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter, AuthenticationFailureResponse failures) throws Exception {
        return http.csrf(csrf -> csrf.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置异常处理，包括认证入口点和访问拒绝处理器
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req,res,e) -> failures.write(res,401,40101,AuthMessageConstant.LOGIN_EXPIRED))
                        .accessDeniedHandler((req,res,e) -> failures.write(res,403,403,AuthMessageConstant.ACCESS_DENIED)))
                // 配置URL访问权限
                .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // 允许匿名访问的URL
                        .requestMatchers(
                                "/controller/auth/**",
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**"
                        ).permitAll()
                        .requestMatchers("/controller/user/activities", "/controller/user/activities/**").permitAll()
                        // 需要特定角色才能访问的URL
                        .requestMatchers("/controller/admin/platform/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers("/controller/admin/organizer/**").hasRole("ORGANIZER_ADMIN")
                        .requestMatchers("/controller/admin/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers("/controller/user/**").hasRole("USER")
                        .requestMatchers("/controller/verify/**").hasRole("VERIFIER")
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated())
                // 在UsernamePasswordAuthenticationFilter之前添加JWT认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
