package com.city.security;
import com.city.config.JacksonConfig;

import com.city.entity.Activity;
import com.city.enums.ActivityStatus;
import com.city.mapper.ActivityMapper;
import com.city.cache.PublicActivityCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real Redis, controlled mapper fixture; never mutates production activity rows. */
class PublicActivityCacheRedisIT {
    LettuceConnectionFactory factory; StringRedisTemplate redis; ActivityMapper mapper; PublicActivityCache cache;
    String prefix; ObjectMapper json=new ObjectMapper().findAndRegisterModules().registerModule(new JacksonConfig().longAsStringModule());
    String data() { return prefix+"{1}:data"; } String lock() { return prefix+"{1}:lock"; }
    @BeforeEach void setup() {
        var yaml=new YamlPropertiesFactoryBean();yaml.setResources(new ClassPathResource("application-dev.yml"));
        var p=Objects.requireNonNull(yaml.getObject());
        var config=new RedisStandaloneConfiguration(p.getProperty("city.redis.host"),Integer.parseInt(p.getProperty("city.redis.port")));
        config.setPassword(p.getProperty("city.redis.password"));config.setDatabase(1);
        factory=new LettuceConnectionFactory(config,LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(4)).shutdownTimeout(Duration.ZERO).build());
        factory.afterPropertiesSet();redis=new StringRedisTemplate(factory);redis.afterPropertiesSet();
        prefix="test:activity-cache:"+UUID.randomUUID()+":";
        mapper=mock(ActivityMapper.class);cache=new PublicActivityCache(redis,json,mapper);
        ReflectionTestUtils.setField(cache,"prefix",prefix);
        when(mapper.isPublished(1L)).thenReturn(true);
        redis.hasKey(data());
    }
    @AfterEach void cleanup() {
        if(TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
        try { redis.delete(List.of(data(),lock())); } finally { factory.destroy(); }
    }
    Activity activity(String title) {
        Activity a=new Activity();a.setId(1L);a.setTitle(title);a.setStatus(ActivityStatus.PUBLISHED);
        a.setDescription("真实业务静态资料");a.setCreatedAt(LocalDateTime.of(2026,10,16,12,0));return a;
    }
    @Test void storesReadableJsonWithJitteredTtlAndReadsLargeMetadataOnlyOnce() {
        when(mapper.selectById(1L)).thenReturn(activity("缓存活动"));
        assertThat(cache.getPublished(1L).getTitle()).isEqualTo("缓存活动");
        assertThat(cache.getPublished(1L).getCreatedAt()).isEqualTo(LocalDateTime.of(2026,10,16,12,0));
        assertThat(redis.opsForValue().get(data())).contains("缓存活动").doesNotContain("\\xac\\xed");
        assertThat(redis.getExpire(data())).isBetween(298L,360L);
        assertThat(redis.hasKey(lock())).isFalse();verify(mapper,times(1)).selectById(1L);verify(mapper,times(2)).isPublished(1L);
    }
    @Test void missingOrPrivateActivityIsNegativelyCachedForAShortTime() {
        when(mapper.selectById(1L)).thenReturn(null);
        assertThat(cache.getPublished(1L)).isNull();assertThat(cache.getPublished(1L)).isNull();
        assertThat(redis.opsForValue().get(data())).isEqualTo("null");assertThat(redis.getExpire(data())).isBetween(18L,40L);
        verify(mapper,times(1)).selectById(1L);
        cache.invalidateAfterCommit(1L);Activity draft=activity("private");draft.setStatus(ActivityStatus.DRAFT);
        when(mapper.selectById(1L)).thenReturn(draft);assertThat(cache.getPublished(1L)).isNull();
        assertThat(redis.opsForValue().get(data())).isEqualTo("null");
    }
    @Test void cachedPublishedMetadataCannotExposeAnOfflineActivity() {
        when(mapper.selectById(1L)).thenReturn(activity("cached"));cache.getPublished(1L);
        when(mapper.isPublished(1L)).thenReturn(false);
        assertThat(cache.getPublished(1L)).isNull();verify(mapper,times(1)).selectById(1L);
    }
    @Test void cacheMissConcurrencyHasOnlyOneDatabaseMetadataBuilder() throws Exception {
        var pool=Executors.newFixedThreadPool(6);var start=new CountDownLatch(1);
        when(mapper.selectById(1L)).thenAnswer(call->{Thread.sleep(45);return activity("one builder");});
        try {
            List<Future<Activity>> futures=new ArrayList<>();
            for(int i=0;i<6;i++) futures.add(pool.submit(()->{start.await();return cache.getPublished(1L);}));
            start.countDown();for(var f:futures) assertThat(f.get(5,TimeUnit.SECONDS).getTitle()).isEqualTo("one builder");
            verify(mapper,times(1)).selectById(1L);
        } finally { pool.shutdownNow(); }
    }
    @Test void busyRebuildDoesNotLetWaitersFallThroughToDatabase() {
        redis.opsForValue().set(lock(),"other owner",Duration.ofSeconds(5));
        assertThatThrownBy(()->cache.getPublished(1L)).hasMessageContaining("稍后重试");
        verify(mapper,never()).selectById(anyLong());assertThat(redis.opsForValue().get(lock())).isEqualTo("other owner");
    }
    @Test void invalidationFencesAReadThatStartedBeforeCommit() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
        when(mapper.selectById(1L)).thenAnswer(call->{entered.countDown();assertThat(release.await(4,TimeUnit.SECONDS)).isTrue();return activity("old");});
        try {
            var pending=pool.submit(()->cache.getPublished(1L));assertThat(entered.await(4,TimeUnit.SECONDS)).isTrue();
            cache.invalidateAfterCommit(1L);release.countDown();pending.get(5,TimeUnit.SECONDS);
            assertThat(redis.hasKey(data())).isFalse();
            when(mapper.selectById(1L)).thenReturn(activity("new"));
            assertThat(cache.getPublished(1L).getTitle()).isEqualTo("new");
        } finally { release.countDown();pool.shutdownNow(); }
    }
    @Test void staleBuilderCannotOverwriteOrReleaseAReplacementLease() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
        when(mapper.selectById(1L)).thenAnswer(call->{entered.countDown();release.await(4,TimeUnit.SECONDS);return activity("old");});
        try {
            var pending=pool.submit(()->cache.getPublished(1L));assertThat(entered.await(4,TimeUnit.SECONDS)).isTrue();
            // Model expiry of the old 5-second lease without slowing the test by five seconds.
            redis.delete(lock());redis.opsForValue().set(lock(),"replacement",Duration.ofSeconds(5));
            release.countDown();pending.get(5,TimeUnit.SECONDS);
            assertThat(redis.hasKey(data())).isFalse();assertThat(redis.opsForValue().get(lock())).isEqualTo("replacement");
        } finally { release.countDown();pool.shutdownNow(); }
    }
    @Test void databaseFailureNeverBecomesANegativeCacheHit() {
        var failure=new DataAccessResourceFailureException("database offline");when(mapper.selectById(1L)).thenThrow(failure);
        assertThatThrownBy(()->cache.getPublished(1L)).isSameAs(failure);
        assertThat(redis.hasKey(data())).isFalse();assertThat(redis.hasKey(lock())).isFalse();
        doReturn(activity("recovered")).when(mapper).selectById(1L);assertThat(cache.getPublished(1L).getTitle()).isEqualTo("recovered");
    }
    @Test void corruptJsonIsDiscardedAndRealMetadataIsReturned() {
        redis.opsForValue().set(data(),"{broken",Duration.ofMinutes(5));when(mapper.selectById(1L)).thenReturn(activity("safe"));
        assertThat(cache.getPublished(1L).getTitle()).isEqualTo("safe");assertThat(redis.hasKey(data())).isFalse();
        assertThat(cache.getPublished(1L).getTitle()).isEqualTo("safe");assertThat(redis.hasKey(data())).isTrue();
    }
    @Test void transactionRollbackKeepsCacheAndCommitDeletesIt() {
        when(mapper.selectById(1L)).thenReturn(activity("before"));cache.getPublished(1L);
        TransactionSynchronizationManager.initSynchronization();cache.invalidateAfterCommit(1L);
        var rollback=TransactionSynchronizationManager.getSynchronizations();rollback.forEach(s->s.afterCompletion(1));
        TransactionSynchronizationManager.clearSynchronization();assertThat(redis.hasKey(data())).isTrue();
        TransactionSynchronizationManager.initSynchronization();cache.invalidateAfterCommit(1L);
        TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCommit());
        assertThat(redis.hasKey(data())).isFalse();
    }
}
