package com.city.security;

import com.city.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenBlacklistServiceTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final JwtProperties properties = new JwtProperties();
    private final TokenBlacklistService service = new TokenBlacklistService();
    @BeforeEach void setup() {
        properties.setBlacklistPrefix("jwt:blacklist:");
        properties.setUserTokensPrefix("jwt:user-tokens:");
        properties.setTtl(Duration.ofHours(12));
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(service, "jwtProperties", properties);
    }
    @SuppressWarnings("unchecked") private void acknowledge() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);
    }
    @Test void registersInOneAtomicScriptInsteadOfSeparateSetAndExpire() {
        acknowledge();
        service.register(7L, "abc", Duration.ofHours(12));
        verify(redis).execute(any(RedisScript.class), eq(List.of("jwt:user-tokens:7")), any(Object[].class));
        verify(redis, never()).opsForSet(); verify(redis, never()).expire(anyString(), any(Duration.class));
    }
    @Test void rejectsNonPositiveRegistrationTtlBeforeRedis() {
        assertThatThrownBy(() -> service.register(7L, "abc", Duration.ZERO)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(redis);
    }
    @Test void revokesTokenInOneAtomicScript() {
        acknowledge(); service.revoke("abc", Duration.ofMinutes(10));
        verify(redis).execute(any(RedisScript.class), eq(List.of("jwt:blacklist:abc")), eq("REVOKE"), eq("LOGOUT"), eq("600000"));
    }
    @Test void ignoresExpiredRevocations() {
        service.revoke("abc", Duration.ZERO); service.revoke("abc", Duration.ofSeconds(-1));
        verifyNoInteractions(redis);
    }
    @Test void calculatesRemainingTtlFromExpiry() {
        acknowledge(); service.revoke("abc", Instant.now().plusSeconds(60));
        verify(redis).execute(any(RedisScript.class), eq(List.of("jwt:blacklist:abc")), eq("REVOKE"), eq("LOGOUT"), argThat((String ms) -> Long.parseLong(ms) > 0 && Long.parseLong(ms) <= 60000));
    }
    @Test void removesSessionInOneAtomicScript() {
        acknowledge(); service.remove(7L, "abc");
        verify(redis).execute(any(RedisScript.class), eq(List.of("jwt:user-tokens:7")), eq("REMOVE"), eq("abc"), eq("43200000"));
    }
    @Test void revokesBatchInOneAtomicScript() {
        acknowledge(); service.revokeAll(7L, RevocationReason.FORCED_LOGOUT);
        verify(redis).execute(any(RedisScript.class), eq(List.of("jwt:user-tokens:7")), eq("REVOKE_ALL"), eq("jwt:blacklist:"), eq("FORCED_LOGOUT"), eq("43200000"));
        verify(redis, never()).delete(anyString());
    }
    @Test void scriptFailurePropagatesWithoutPretendingSuccess() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenThrow(new DataAccessResourceFailureException("unavailable"));
        assertThatThrownBy(() -> service.revokeAll(7L, RevocationReason.FORCED_LOGOUT)).isInstanceOf(DataAccessException.class);
        verify(redis, never()).delete(anyString());
    }
    @Test void unconfirmedScriptResultIsFailure() {
        assertThatThrownBy(() -> service.revokeAll(7L, RevocationReason.FORCED_LOGOUT)).isInstanceOf(DataAccessException.class);
    }
    @Test void readsLegacyAndExplicitReasons() {
        ValueOperations<String,String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("jwt:blacklist:abc")).thenReturn("1", "FORCED_LOGOUT", "ACCOUNT_DISABLED", null);
        assertThat(service.reason("abc")).isEqualTo(RevocationReason.LEGACY);
        assertThat(service.reason("abc")).isEqualTo(RevocationReason.FORCED_LOGOUT);
        assertThat(service.reason("abc")).isEqualTo(RevocationReason.ACCOUNT_DISABLED);
        assertThat(service.isRevoked("abc")).isFalse();
    }
}
