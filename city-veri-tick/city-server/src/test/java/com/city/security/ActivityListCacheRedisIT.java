package com.city.security;

import com.city.cache.ActivityListCache;
import com.city.entity.Activity;
import com.city.enums.ActivityStatus;
import com.city.mapper.ActivityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityListCacheRedisIT {
 LettuceConnectionFactory factory;StringRedisTemplate redis;ActivityMapper mapper;ActivityListCache cache;String prefix;
 @BeforeEach void setup(){var y=new YamlPropertiesFactoryBean();y.setResources(new ClassPathResource("application-dev.yml"));var p=Objects.requireNonNull(y.getObject());var c=new RedisStandaloneConfiguration(p.getProperty("city.redis.host"),Integer.parseInt(p.getProperty("city.redis.port")));c.setPassword(p.getProperty("city.redis.password"));c.setDatabase(1);factory=new LettuceConnectionFactory(c,LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(4)).shutdownTimeout(Duration.ZERO).build());factory.afterPropertiesSet();redis=new StringRedisTemplate(factory);redis.afterPropertiesSet();prefix="test:activity-list:"+UUID.randomUUID()+":";mapper=mock(ActivityMapper.class);cache=new ActivityListCache(redis,new ObjectMapper().findAndRegisterModules(),mapper);ReflectionTestUtils.setField(cache,"prefix",prefix);}
 @AfterEach void cleanup(){try{var keys=redis.keys(prefix+"*");if(keys!=null&&!keys.isEmpty())redis.delete(keys);}finally{factory.destroy();}}
 Activity a(){var x=new Activity();x.setId(1L);x.setTitle("列表活动");x.setStatus(ActivityStatus.PUBLISHED);return x;}
 @Test void cachesJsonCandidateListWithBoundedTtlAndUsesCurrentStatus(){when(mapper.isPublished(1L)).thenReturn(true);var first=cache.getPublished("音乐","演出",()->List.of(a()));assertThat(first).hasSize(1);assertThat(cache.getPublished("音乐","演出",()->{throw new AssertionError("should be cache hit");})).hasSize(1);assertThat(redis.keys(prefix+"*").stream().filter(k->!k.endsWith("version")).findFirst().map(redis::getExpire).orElse(0L)).isBetween(28L,60L);}
 @Test void versionIncrementMakesOldQueryKeyMiss(){when(mapper.isPublished(1L)).thenReturn(true);cache.getPublished(null,null,()->List.of(a()));cache.invalidateAfterCommit();var fresh=a();fresh.setTitle("新列表活动");assertThat(cache.getPublished(null,null,()->List.of(fresh)).get(0).getTitle()).isEqualTo("新列表活动");}
}
