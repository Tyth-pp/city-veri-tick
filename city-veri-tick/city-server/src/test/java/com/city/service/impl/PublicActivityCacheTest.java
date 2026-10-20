package com.city.service.impl;
import com.city.cache.PublicActivityCache;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.city.entity.Activity;
import com.city.enums.ActivityStatus;
import com.city.mapper.ActivityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PublicActivityCacheTest {
    StringRedisTemplate redis; ValueOperations<String,String> values; ActivityMapper mapper; PublicActivityCache cache;
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        redis=mock(StringRedisTemplate.class); values=mock(ValueOperations.class); mapper=mock(ActivityMapper.class);
        when(redis.opsForValue()).thenReturn(values);
        cache=new PublicActivityCache(redis,new ObjectMapper().findAndRegisterModules(),mapper);
    }
    @AfterEach void cleanup() { if(TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization(); }
    @Test void invalidIdDoesNotHitRedisOrDatabase() {
        assertThatThrownBy(()->cache.getPublished(0L)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(redis,mapper);
    }
    @Test void negativeCacheDoesNotHitDatabase() {
        when(values.get(anyString())).thenReturn("null");
        assertThat(cache.getPublished(1L)).isNull(); verifyNoInteractions(mapper);
    }
    @Test void cachedActivityStillChecksPublishedStatusWithoutReadingLargeContent() throws Exception {
        Activity a=new Activity();a.setId(1L);a.setStatus(ActivityStatus.PUBLISHED);a.setTitle("已缓存内容");
        when(values.get(anyString())).thenReturn(new ObjectMapper().findAndRegisterModules().writeValueAsString(a));
        when(mapper.isPublished(1L)).thenReturn(false);
        assertThat(cache.getPublished(1L)).isNull(); verify(mapper,never()).selectById(anyLong());
    }
    @Test void redisUnavailableFallsBackToDatabaseAndBrieflySkipsFurtherRedisCalls() {
        when(values.get(anyString())).thenThrow(new DataAccessResourceFailureException("offline"));
        Activity a=new Activity();a.setId(1L);a.setStatus(ActivityStatus.PUBLISHED);
        when(mapper.selectById(1L)).thenReturn(a);when(mapper.isPublished(1L)).thenReturn(true);
        assertThat(cache.getPublished(1L)).isEqualTo(a);assertThat(cache.getPublished(1L)).isEqualTo(a);
        verify(values,times(1)).get(anyString());verify(mapper,times(2)).selectById(1L);
    }
    @Test void invalidationWaitsForCommitAndDoesNotRunOnRollback() {
        TransactionSynchronizationManager.initSynchronization();cache.invalidateAfterCommit(1L);
        verifyNoInteractions(redis);
        var callbacks=TransactionSynchronizationManager.getSynchronizations(); assertThat(callbacks).hasSize(1);
        callbacks.get(0).afterCompletion(1);verifyNoInteractions(redis);
        callbacks.get(0).afterCommit();verify(redis).execute(any(),anyList(),any(Object[].class));
    }
    @Test void failedInvalidationDoesNotTurnCommittedBusinessIntoFailure() {
        when(redis.execute(any(),anyList(),any(Object[].class))).thenThrow(new DataAccessResourceFailureException("offline"));
        assertThatCode(()->cache.invalidateAfterCommit(1L)).doesNotThrowAnyException();
    }

    @Test void unavailableRedisCannotCreateUnboundedConcurrentDatabaseLoads() throws Exception {
        when(values.get(anyString())).thenThrow(new DataAccessResourceFailureException("offline"));
        var entered=new CountDownLatch(8);
        var release=new CountDownLatch(1);
        when(mapper.selectById(anyLong())).thenAnswer(call->{
            entered.countDown();
            if(!release.await(4,TimeUnit.SECONDS)) throw new AssertionError("test release timeout");
            return null;
        });
        var pool=Executors.newFixedThreadPool(8);
        try {
            var work=new ArrayList<Future<Activity>>();
            for(long id=1;id<=8;id++) { final long activityId=id;work.add(pool.submit(()->cache.getPublished(activityId))); }
            assertThat(entered.await(4,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(()->cache.getPublished(9L)).hasMessageContaining("稍后重试");
            verify(mapper,never()).selectById(9L);
            release.countDown();for(var job:work) assertThat(job.get(4,TimeUnit.SECONDS)).isNull();
        } finally { release.countDown();pool.shutdownNow(); }
    }
    @Test void visibilityDatabaseFailureDoesNotFallBackOrExposeCachedActivity() throws Exception {
        Activity a=new Activity();a.setId(1L);a.setStatus(ActivityStatus.PUBLISHED);
        when(values.get(anyString())).thenReturn(new ObjectMapper().findAndRegisterModules().writeValueAsString(a));
        var failure=new DataAccessResourceFailureException("database unavailable");
        when(mapper.isPublished(1L)).thenThrow(failure);
        assertThatThrownBy(()->cache.getPublished(1L)).isSameAs(failure);
        verify(mapper,never()).selectById(anyLong());
    }
}
