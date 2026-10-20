package com.city.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import com.city.properties.JwtProperties;

/**
 * JWT令牌服务类，用于创建和解析JWT令牌
 */
public class JwtTokenService {
    // 用于签名JWT的密钥
    private final SecretKey key;
    // 令牌的有效期
    private final Duration ttl;
    // 私有最终字符串变量，用于表示发行者
    private final String issuer;
    // 时钟对象，用于获取当前时间
    private final Clock clock;

    /**
     * 构造函数，初始化JWT令牌服务
     * @param base64Secret Base64编码的密钥字符串
     * @param ttl 令牌的有效期
     * @param clock 时钟对象
     */
    public JwtTokenService(String base64Secret, Duration ttl, Clock clock) {
        this(base64Secret, ttl, "city-veri-tick", clock);
    }

    private JwtTokenService(String base64Secret, Duration ttl, String issuer, Clock clock) {
        // 将Base64编码的密钥解码并创建HMAC密钥
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
        this.ttl = ttl;
        this.issuer = issuer;
        this.clock = clock;
    }

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this(properties.getSecret(), properties.getTtl(), properties.getIssuer(), clock);
    }
    /**
     * 创建JWT令牌
     * @param userId 用户ID
     * @param role 用户角色
     * @return 生成的JWT令牌字符串
     */
    public String createToken(Long userId, String role) {
        return createToken(userId,role,false);
    }
    public String createInitialPasswordToken(Long userId,String role) {
        return createToken(userId,role,true);
    }
    private String createToken(Long userId,String role,boolean initialPasswordOnly) {
        // 获取当前时间作为签发时间
        Date issuedAt = Date.from(clock.instant());
        // 构建并返回JWT令牌
        return Jwts.builder().id(UUID.randomUUID().toString()).issuer(issuer).subject(String.valueOf(userId)).claim("role", role).claim("initialPasswordOnly",initialPasswordOnly).issuedAt(issuedAt)
                // 设置过期时间为当前时间加上有效期
                .expiration(Date.from(clock.instant().plus(ttl))).signWith(key).compact();
    }

    /**
     * 解析JWT令牌
     * @param token JWT令牌字符串
     * @ 包含用户信息的Optional对象，如果解析失败则返回空Optional
     */
    public Optional<LoginPrincipal> parse(String token) {
        try {
            // 解析JWT令牌并获取声明
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(issuer).clock(() -> Date.from(clock.instant())).build()
                    .parseSignedClaims(token).getPayload();
            // 创建并返回LoginPrincipal对象
            return Optional.of(new LoginPrincipal(Long.valueOf(claims.getSubject()), claims.get("role", String.class),
                    claims.getId(), claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant(),Boolean.TRUE.equals(claims.get("initialPasswordOnly",Boolean.class))));
        } catch (RuntimeException exception) {
            // 如果解析过程中出现异常，返回空Optional
            return Optional.empty();
        }
    }
}
