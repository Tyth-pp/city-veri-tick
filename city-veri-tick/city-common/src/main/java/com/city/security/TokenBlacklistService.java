package com.city.security;

import com.city.properties.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class TokenBlacklistService {
    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final DefaultRedisScript<Long> SESSIONS = sessionScript();

    private static DefaultRedisScript<Long> sessionScript() {
        var script = new DefaultRedisScript<Long>();
        script.setLocation(new ClassPathResource("redis/token-sessions.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JwtProperties jwtProperties;

    public void register(Long userId, String jti, Duration ttl) {
        requirePositiveTtl(ttl);
        register(userId, jti, Instant.now().plus(ttl));
    }

    public void register(Long userId, String jti, Instant expiresAt) {
        requireIdentity(userId, jti);
        Objects.requireNonNull(expiresAt, "JWT expiry is required");
        long result = execute(jwtProperties.getUserTokensPrefix() + userId, "REGISTER", jti,
                Long.toString(expiresAt.toEpochMilli()), Long.toString(fallbackTtl().toMillis()));
        if (result != 1) throw new DataAccessResourceFailureException("JWT expired before session registration");
    }

    /**
     * Removes only this session; the blacklist remains until the token expires.
     */
    public void remove(Long userId, String jti) {
        requireIdentity(userId, jti);
        execute(jwtProperties.getUserTokensPrefix() + userId, "REMOVE", jti, Long.toString(fallbackTtl().toMillis()));
    }

    public void revoke(String jti, Duration ttl) {
        revoke(jti, ttl, RevocationReason.LOGOUT);
    }

    public void revoke(String jti, Duration ttl, RevocationReason reason) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) return;
        requireIdentity(0L, jti);
        execute(jwtProperties.getBlacklistPrefix() + jti, "REVOKE", Objects.requireNonNull(reason).name(),
                Long.toString(Math.max(1, ttl.toMillis())));
    }

    public void revoke(String jti, Instant expiresAt) {
        revoke(jti, Duration.between(Instant.now(), expiresAt));
    }

    public boolean isRevoked(String jti) {
        return reason(jti) != null;
    }

    public RevocationReason reason(String jti) {
        return RevocationReason.fromValue(stringRedisTemplate.opsForValue().get(jwtProperties.getBlacklistPrefix() + jti));
    }

    public void revokeAll(Long userId, Duration ttl) {
        revokeAll(userId, ttl, RevocationReason.FORCED_LOGOUT);
    }

    public void revokeAll(Long userId, RevocationReason reason) {
        revokeAll(userId, fallbackTtl(), reason);
    }

    private void revokeAll(Long userId, Duration ttl, RevocationReason reason) {
        if (userId == null) throw new IllegalArgumentException("User id is required");
        requirePositiveTtl(ttl);
        // Known ZSet members use their own expiry. Legacy Set members use the conservative fallback.
        execute(jwtProperties.getUserTokensPrefix() + userId, "REVOKE_ALL", jwtProperties.getBlacklistPrefix(),
                Objects.requireNonNull(reason).name(), Long.toString(Math.max(ttl.toMillis(), fallbackTtl().toMillis())));
    }

    private long execute(String key, String operation, String... arguments) {
        Object[] args = new Object[arguments.length + 1];
        args[0] = operation;
        System.arraycopy(arguments, 0, args, 1, arguments.length);
        try {
            Long value = stringRedisTemplate.execute(SESSIONS, List.of(key), args);
            if (value == null)
                throw new DataAccessResourceFailureException("Redis did not confirm JWT session operation");
            return value;
        } catch (DataAccessException e) {
            // Never log JWTs, JTIs, credentials or command arguments.
            log.warn("JWT Redis operation failed: operation={}, failure={}", operation, e.getClass().getSimpleName());
            throw e;
        }
    }

    private Duration fallbackTtl() {
        Duration ttl = jwtProperties.getTtl();
        requirePositiveTtl(ttl);
        return ttl;
    }

    private void requirePositiveTtl(Duration ttl) {
        if (ttl == null || ttl.toMillis() <= 0) throw new IllegalStateException("JWT TTL must be positive");
    }

    private void requireIdentity(Long userId, String jti) {
        if (userId == null || jti == null || jti.isBlank())
            throw new IllegalArgumentException("JWT session identity is required");
    }
}
