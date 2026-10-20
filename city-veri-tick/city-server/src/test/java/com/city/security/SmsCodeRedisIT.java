package com.city.security;
import com.city.service.impl.SmsCodeServiceImpl;

import com.city.service.SmsCodeService;
import com.city.service.SmsCodeSender;
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
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmsCodeRedisIT {
    private LettuceConnectionFactory factory;
    private StringRedisTemplate redis;
    private SmsCodeService service;
    private SmsCodeSender sender;
    private String prefix;
    private final String phone = "13800138000";
    private String k(String type) { return prefix + "{" + phone + "}:" + type; }
    @BeforeEach void setup() {
        var yaml=new YamlPropertiesFactoryBean(); yaml.setResources(new ClassPathResource("application-dev.yml"));
        var p=Objects.requireNonNull(yaml.getObject());
        var config=new RedisStandaloneConfiguration(p.getProperty("city.redis.host"),Integer.parseInt(p.getProperty("city.redis.port")));
        config.setPassword(p.getProperty("city.redis.password")); config.setDatabase(1);
        factory=new LettuceConnectionFactory(config,LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(4)).shutdownTimeout(Duration.ZERO).build());
        factory.afterPropertiesSet(); redis=new StringRedisTemplate(factory); redis.afterPropertiesSet();
        prefix="test:sms:"+UUID.randomUUID()+":";
        sender=mock(SmsCodeSender.class); service=new SmsCodeServiceImpl();
        ReflectionTestUtils.setField(service,"redis",redis);
        ReflectionTestUtils.setField(service,"prefix",prefix);
        ReflectionTestUtils.setField(service,"sender",sender);
    }
    @AfterEach void cleanup() {
        try { redis.delete(List.of(k("code"),k("cooldown"),k("fail"),k("lease"))); }
        finally { factory.destroy(); }
    }
    private String issue() { service.send(phone); return redis.opsForValue().get(k("code")); }
    @Test void sendsOnceWithBoundedTtlAndDoesNotReplaceCodeDuringCooldown() {
        String code=issue(); assertThat(code).matches("[0-9]{6}");
        assertThat(redis.getExpire(k("code"))).isBetween(295L,300L);
        assertThat(redis.getExpire(k("cooldown"))).isBetween(55L,60L);
        assertThatThrownBy(()->service.send(phone)).hasMessageContaining("频繁");
        assertThat(redis.opsForValue().get(k("code"))).isEqualTo(code);
        verify(sender,times(1)).send(eq(phone),eq(code),eq(300));
    }
    @Test void successfulVerificationIsConsumedExactlyOnceAndClearsFailures() {
        String code=issue();
        assertThatThrownBy(()->service.reserve(phone,code.equals("000000")?"111111":"000000")).hasMessageContaining("验证码");
        String lease=service.reserve(phone,code);
        service.complete(phone,lease);
        assertThat(redis.hasKey(k("code"))).isFalse(); assertThat(redis.hasKey(k("fail"))).isFalse();
        assertThatThrownBy(()->service.reserve(phone,code)).hasMessageContaining("验证码");
    }
    @Test void fiveWrongAttemptsTemporarilyBlockEvenTheCorrectCode() {
        String code=issue(); String wrong=code.equals("000000")?"111111":"000000";
        for(int i=0;i<5;i++) assertThatThrownBy(()->service.reserve(phone,wrong)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->service.reserve(phone,code)).hasMessageContaining("次数");
        assertThatThrownBy(()->service.send(phone)).hasMessageContaining("次数");
        assertThat(redis.getExpire(k("fail"))).isBetween(595L,600L);
    }
    @Test void failedBusinessCanReleaseReservationWithoutConsumingTheCode() {
        String code=issue(); String lease=service.reserve(phone,code);
        service.release(phone,lease);
        String retry=service.reserve(phone,code);
        service.complete(phone,retry);
        assertThat(redis.hasKey(k("code"))).isFalse();
    }
    @Test void concurrentVerificationOnlyLetsOneRequestReserveTheCode() throws Exception {
        String code=issue(); var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            Callable<String> attempt=()->{start.await();try{return service.reserve(phone,code);}catch(RuntimeException e){return null;}};
            var first=pool.submit(attempt); var second=pool.submit(attempt); start.countDown();
            var results=Arrays.asList(first.get(5,TimeUnit.SECONDS),second.get(5,TimeUnit.SECONDS));
            assertThat(results.stream().filter(Objects::nonNull).count()).isEqualTo(1);
            service.complete(phone,results.stream().filter(Objects::nonNull).findFirst().orElseThrow());
        } finally { pool.shutdownNow(); }
    }
    @Test void staleReservationCannotConsumeOrReleaseAReplacementReservation() {
        String code=issue(); String old=service.reserve(phone,code); redis.delete(k("lease"));
        String current=service.reserve(phone,code); service.release(phone,old);
        assertThat(redis.opsForValue().get(k("lease"))).isEqualTo(current);
        assertThatThrownBy(()->service.complete(phone,old)).hasMessageContaining("验证码");
        service.complete(phone,current);
    }
    @Test void expiredCodeCannotBeUsedAndSenderFailureReleasesOnlyOwnPendingCode() {
        issue(); redis.delete(k("code"));
        assertThatThrownBy(()->service.reserve(phone,"123456")).hasMessageContaining("验证码");
        redis.delete(k("cooldown"));
        doThrow(new IllegalStateException("provider unavailable")).when(sender).send(anyString(),anyString(),anyInt());
        assertThatThrownBy(()->service.send(phone)).isInstanceOf(RuntimeException.class);
        assertThat(redis.hasKey(k("code"))).isFalse(); assertThat(redis.hasKey(k("cooldown"))).isFalse();
    }
    @Test void missingSmsChannelDoesNotCreateACodeOrPretendDeliverySucceeded() {
        ReflectionTestUtils.setField(service,"sender",null);
        assertThatThrownBy(()->service.send(phone)).hasMessageContaining("短信服务");
        assertThat(redis.hasKey(k("code"))).isFalse();
    }
}
