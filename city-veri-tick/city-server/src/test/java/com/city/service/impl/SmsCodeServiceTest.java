package com.city.service.impl;
import com.city.service.SmsCodeService;
import com.city.integration.sms.DevelopmentSmsCodeSender;
import org.springframework.context.annotation.Profile;

import com.city.service.SmsCodeSender;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmsCodeServiceTest {
    private final SmsCodeService service=new SmsCodeServiceImpl();
    private final StringRedisTemplate redis=mock(StringRedisTemplate.class);
    private final SmsCodeSender sender=mock(SmsCodeSender.class);
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service,"redis",redis);
        ReflectionTestUtils.setField(service,"sender",sender);
    }
    @Test void redisFailureNeverPretendsAnSmsWasSent() {
        when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenThrow(new DataAccessResourceFailureException("unavailable"));
        assertThatThrownBy(()->service.send("13800138000")).isInstanceOf(DataAccessResourceFailureException.class);
        verifyNoInteractions(sender);
    }
    @Test void cooldownAndAttemptLimitsDoNotCallDeliveryChannel() {
        when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenReturn(-1L,-2L);
        assertThatThrownBy(()->service.send("13800138000")).hasMessageContaining("频繁");
        assertThatThrownBy(()->service.send("13800138000")).hasMessageContaining("次数");
        verifyNoInteractions(sender);
    }
    @Test void invalidPhoneNeverTouchesRedis() {
        assertThatThrownBy(()->service.send("invalid")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(redis,sender);
    }
    @Test void developmentSenderIsOnlyRegisteredForDevProfile() {
        assertThat(DevelopmentSmsCodeSender.class.getAnnotation(Profile.class).value()).containsExactly("dev");
    }
}
