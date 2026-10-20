package com.city.service.impl;

import com.city.cache.ActivityListCache;
import com.city.entity.Activity;
import com.city.enums.ActivityStatus;
import com.city.mapper.ActivityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.ArgumentCaptor;

class ActivityListCacheTest {
 StringRedisTemplate redis; ValueOperations<String,String> values; ActivityMapper mapper; ActivityListCache cache;
 @BeforeEach @SuppressWarnings("unchecked") void setup(){redis=mock(StringRedisTemplate.class);values=mock(ValueOperations.class);mapper=mock(ActivityMapper.class);when(redis.opsForValue()).thenReturn(values);cache=new ActivityListCache(redis,new ObjectMapper().findAndRegisterModules(),mapper);ReflectionTestUtils.setField(cache,"prefix","test:list:");}
 @AfterEach void cleanup(){if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.clearSynchronization();}
 Activity activity(long id,ActivityStatus status){Activity a=new Activity();a.setId(id);a.setStatus(status);a.setTitle("活动"+id);return a;}
 @Test void cacheHitChecksCurrentPublishedStatus(){when(values.get(anyString())).thenReturn("[{\"id\":1,\"title\":\"活动1\",\"status\":\"PUBLISHED\"}]");when(mapper.isPublished(1L)).thenReturn(false);assertThat(cache.getPublished("音乐","演出",()->List.of(activity(1,ActivityStatus.PUBLISHED)))).isEmpty();verify(mapper,never()).selectById(anyLong());}
 @Test void missWritesJsonWithShortRandomTtlAndDoesNotUseJdkSerialization(){when(values.get(anyString())).thenAnswer(i->i.getArgument(0,String.class).endsWith("version")?"1":null);when(values.setIfAbsent(anyString(),eq("1"))).thenReturn(true);List<Activity> result=cache.getPublished("  音乐  ","演出",()->List.of(activity(1,ActivityStatus.PUBLISHED)));assertThat(result).hasSize(1);verify(values).set(anyString(),contains("活动1"),any());}
 @Test void redisFailureFallsBackToDatabaseLoader(){when(values.get(anyString())).thenThrow(new DataAccessResourceFailureException("offline"));var loaded=List.of(activity(1,ActivityStatus.PUBLISHED));assertThat(cache.getPublished(null,null,()->loaded)).isSameAs(loaded);}
 @Test void versionInvalidationRunsOnlyAfterCommit(){TransactionSynchronizationManager.initSynchronization();cache.invalidateAfterCommit();verify(values,never()).increment(anyString());var callbacks=TransactionSynchronizationManager.getSynchronizations();callbacks.get(0).afterCompletion(1);verify(values,never()).increment(anyString());TransactionSynchronizationManager.clearSynchronization();TransactionSynchronizationManager.initSynchronization();cache.invalidateAfterCommit();TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();verify(values).increment(anyString());}
 @Test void longInputsProduceBoundedKeyParts(){when(values.get(anyString())).thenReturn(null,"1");String huge="x".repeat(500);cache.getPublished(huge,null,()->List.of());ArgumentCaptor<String> captor=ArgumentCaptor.forClass(String.class);verify(values,atLeastOnce()).get(captor.capture());assertThat(captor.getAllValues().stream().filter(k->k.startsWith("test:list:")&&!k.endsWith(":version")).mapToInt(String::length).max().orElse(0)).isLessThan(160);}

 @Test void distinctLongInputsDoNotShareAKey(){when(values.get(anyString())).thenReturn(null,"1");String first="a".repeat(300)+"x";String second="a".repeat(300)+"y";cache.getPublished(first,null,()->List.of());cache.getPublished(second,null,()->List.of());ArgumentCaptor<String> captor=ArgumentCaptor.forClass(String.class);verify(values,atLeast(2)).get(captor.capture());var keys=captor.getAllValues().stream().filter(k->k.startsWith("test:list:")&&!k.endsWith(":version")).toList();assertThat(keys).hasSizeGreaterThanOrEqualTo(2).doesNotHaveDuplicates();}

 @Test void corruptJsonFallsBackToLoader(){when(values.get(anyString())).thenReturn("{bad");var loaded=List.of(activity(2,ActivityStatus.PUBLISHED));assertThat(cache.getPublished(null,null,()->loaded)).containsExactlyElementsOf(loaded);}
}
