package com.city.security;
import org.springframework.dao.DataAccessException;

import com.city.properties.JwtProperties;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Explicit integration test against configured Redis; touches only a unique test namespace. */
class TokenBlacklistRedisIT {
    private LettuceConnectionFactory factory;
    private StringRedisTemplate redis;
    private TokenBlacklistService service;
    private String namespace;
    private final Set<String> keys = new HashSet<>();

    @BeforeEach void connect() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-dev.yml"));
        var p = Objects.requireNonNull(yaml.getObject());
        var config = new RedisStandaloneConfiguration(p.getProperty("city.redis.host"), Integer.parseInt(p.getProperty("city.redis.port")));
        String password = p.getProperty("city.redis.password");
        if (password != null && !password.isBlank()) config.setPassword(password);
        config.setDatabase(1);
        factory = new LettuceConnectionFactory(config, LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(4)).shutdownTimeout(Duration.ZERO).build());
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory); redis.afterPropertiesSet();
        namespace = "test:jwt:" + UUID.randomUUID() + ":";
        var properties = new JwtProperties();
        properties.setTtl(Duration.ofSeconds(120));
        properties.setUserTokensPrefix(namespace + "sessions:");
        properties.setBlacklistPrefix(namespace + "blacklist:");
        service = new TokenBlacklistService();
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(service, "jwtProperties", properties);
    }
    private String session() { return key("sessions:7"); }
    private String blacklist(String jti) { return key("blacklist:" + jti); }
    private String key(String suffix) { String k = namespace + suffix; keys.add(k); return k; }
    @AfterEach void cleanup() {
        try { if (redis != null && !keys.isEmpty()) redis.delete(keys); }
        finally { if (factory != null) factory.destroy(); }
    }

    // These integration fixtures exercise absolute expiry against Redis TIME, independent of host clock skew.
    private java.time.Instant redisNow() {
        try(var connection=factory.getConnection()) { return java.time.Instant.ofEpochMilli(connection.serverCommands().time()); }
    }
    @Test void shorterNewTokenDoesNotExpireLongTokenIndexAndExpiredMembersArePruned() {
        var now=redisNow();
        service.register(7L, "long", now.plusSeconds(120));
        assertThat(redis.type(session()).code()).isEqualTo("zset");
        redis.opsForZSet().add(session(), "expired", now.minusSeconds(1).toEpochMilli());
        service.register(7L, "short", now.plusSeconds(10));
        assertThat(redis.opsForZSet().range(session(), 0, -1)).containsExactly("short", "long");
        assertThat(redis.getExpire(session())).isBetween(115L, 120L);
    }
    @Test void legacySetMigratesWithoutLosingOldTokensOrShorteningItsTtl() {
        redis.opsForSet().add(session(), "legacy"); redis.expire(session(), Duration.ofSeconds(120));
        service.register(7L, "new", Duration.ofSeconds(10));
        assertThat(redis.type(session()).code()).isEqualTo("zset");
        assertThat(redis.opsForZSet().range(session(), 0, -1)).contains("legacy", "new");
        assertThat(redis.getExpire(session())).isBetween(115L, 120L);
        blacklist("legacy"); blacklist("new");
        service.revokeAll(7L, RevocationReason.FORCED_LOGOUT);
        assertThat(service.reason("legacy")).isEqualTo(RevocationReason.FORCED_LOGOUT);
        assertThat(service.reason("new")).isEqualTo(RevocationReason.FORCED_LOGOUT);
    }
    @Test void legacySetWithPrematureShortTtlKeepsConservativeConfiguredLifetime() {
        redis.opsForSet().add(session(), "legacy-long"); redis.expire(session(), Duration.ofSeconds(10));
        service.register(7L, "short", Duration.ofSeconds(10));
        assertThat(redis.getExpire(session())).isBetween(115L, 120L);
        blacklist("legacy-long"); blacklist("short");
        service.revokeAll(7L, RevocationReason.FORCED_LOGOUT);
        assertThat(redis.getExpire(blacklist("legacy-long"))).isBetween(115L, 120L);
    }
    @Test void persistentLegacySetGetsBoundedLifetimeOnMigration() {
        redis.opsForSet().add(session(), "legacy");
        service.register(7L, "short", Duration.ofSeconds(10));
        assertThat(redis.getExpire(session())).isBetween(115L, 120L);
    }
    @Test void forcedLogoutUsesActualRemainingLifetimesAndCanBeRetried() {
        session(); blacklist("long"); blacklist("short"); blacklist("new");
        var now=redisNow();
        service.register(7L, "long", now.plusSeconds(120));
        service.register(7L, "short", now.plusSeconds(10));
        service.revokeAll(7L, Duration.ofSeconds(5));
        assertThat(redis.getExpire(blacklist("long"))).isBetween(115L, 120L);
        assertThat(redis.getExpire(blacklist("short"))).isBetween(5L, 10L);
        assertThat(redis.hasKey(session())).isFalse();
        service.revokeAll(7L, Duration.ofSeconds(5));
        assertThat(service.reason("long")).isEqualTo(RevocationReason.FORCED_LOGOUT);
        service.register(7L, "new", Duration.ofSeconds(20));
        assertThat(service.reason("new")).isNull();
    }
    @Test void badBlacklistKeyTypeFailsBeforeAnyRevocationAndPreservesIndexForRetry() {
        session(); blacklist("good"); blacklist("bad");
        service.register(7L, "good", Duration.ofSeconds(120));
        service.register(7L, "bad", Duration.ofSeconds(120));
        redis.opsForSet().add(blacklist("bad"), "corrupt");
        assertThatThrownBy(() -> service.revokeAll(7L, Duration.ofSeconds(120))).isInstanceOf(DataAccessException.class);
        assertThat(redis.hasKey(blacklist("good"))).isFalse();
        assertThat(redis.opsForZSet().zCard(session())).isEqualTo(2);
        redis.delete(blacklist("bad"));
        service.revokeAll(7L, Duration.ofSeconds(120));
        assertThat(service.reason("good")).isEqualTo(RevocationReason.FORCED_LOGOUT);
        assertThat(service.reason("bad")).isEqualTo(RevocationReason.FORCED_LOGOUT);
    }
    @Test void ordinaryLogoutPreservesAdminReasonAndNeverShortensBlacklistLifetime() {
        blacklist("token"); session();
        service.register(7L, "token", Duration.ofSeconds(120));
        service.revoke("token", Duration.ofSeconds(120), RevocationReason.ACCOUNT_DISABLED);
        service.revoke("token", Duration.ofSeconds(10));
        service.remove(7L, "token");
        assertThat(service.reason("token")).isEqualTo(RevocationReason.ACCOUNT_DISABLED);
        assertThat(redis.getExpire(blacklist("token"))).isBetween(115L, 120L);
        assertThat(redis.hasKey(session())).isFalse();
    }
    @Test void bulkRevocationAlsoSupportsUnmigratedSets() {
        redis.opsForSet().add(session(), "legacy"); redis.expire(session(), Duration.ofSeconds(120));
        blacklist("legacy");
        service.revokeAll(7L, Duration.ofSeconds(5));
        assertThat(service.reason("legacy")).isEqualTo(RevocationReason.FORCED_LOGOUT);
        assertThat(redis.getExpire(blacklist("legacy"))).isBetween(115L, 120L);
    }
    @Test void removalAlsoSupportsLegacySets() {
        redis.opsForSet().add(session(), "old", "other"); redis.expire(session(), Duration.ofSeconds(60));
        service.remove(7L, "old");
        assertThat(redis.opsForSet().members(session())).containsExactly("other");
    }
    @Test void recordsPersistenceAndEvictionConfigurationWithoutChangingIt() {
        try (var connection = factory.getConnection()) {
            Properties persistence = connection.serverCommands().info("persistence");
            assertThat(persistence).isNotNull();
            System.out.println("Redis persistence: aof_enabled=" + persistence.getProperty("aof_enabled") + ", rdb_last_bgsave_status=" + persistence.getProperty("rdb_last_bgsave_status"));
            for (String option : List.of("appendonly", "appendfsync", "save", "maxmemory-policy")) {
                try { System.out.println("Redis config " + option + "=" + connection.serverCommands().getConfig(option).getProperty(option)); }
                catch (DataAccessException denied) { System.out.println("Redis config " + option + "=not readable with application credentials"); }
            }
        }
    }
}
