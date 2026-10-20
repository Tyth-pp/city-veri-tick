package com.city.security;

import com.city.exception.BusinessException;
import com.city.cache.CouponClaimGate;
import com.city.entity.Coupon;
import com.city.mapper.CouponMapper;
import com.city.mapper.UserCouponMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CouponClaimGateRedisIT {
 LettuceConnectionFactory factory; StringRedisTemplate redis; CouponMapper coupons; UserCouponMapper users; CouponClaimGate gate; String prefix; Coupon coupon;
 @BeforeEach void setup(){
  var yaml=new YamlPropertiesFactoryBean();yaml.setResources(new ClassPathResource("application-dev.yml"));var p=Objects.requireNonNull(yaml.getObject());
  var cfg=new RedisStandaloneConfiguration(p.getProperty("city.redis.host"),Integer.parseInt(p.getProperty("city.redis.port")));cfg.setPassword(p.getProperty("city.redis.password"));cfg.setDatabase(1);
  factory=new LettuceConnectionFactory(cfg,LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(4)).shutdownTimeout(Duration.ZERO).build());factory.afterPropertiesSet();redis=new StringRedisTemplate(factory);redis.afterPropertiesSet();
  prefix="test:coupon-claim:"+UUID.randomUUID()+":";coupons=mock(CouponMapper.class);users=mock(UserCouponMapper.class);
  var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenAnswer(i->new SimpleTransactionStatus());
  gate=new CouponClaimGate(redis,coupons,users,manager,Clock.system(ZoneId.of("Asia/Shanghai")));ReflectionTestUtils.setField(gate,"prefix",prefix);
  coupon=new Coupon();coupon.setId(9L);coupon.setClaimMode("FLASH_SALE");coupon.setStatus("ACTIVE");coupon.setTotalQuantity(10);coupon.setClaimedQuantity(0);coupon.setPerUserLimit(1);
  coupon.setStartsAt(LocalDateTime.now().minusDays(1));coupon.setEndsAt(LocalDateTime.now().plusDays(1));when(coupons.selectForUpdate(9L)).thenReturn(coupon);
 }
 @AfterEach void cleanup(){try{var keys=redis.keys(prefix+"*");if(keys!=null&&!keys.isEmpty())redis.delete(keys);}finally{factory.destroy();}}
 @Test void concurrentClaimsCannotExceedStock() throws Exception {
  var pool=Executors.newFixedThreadPool(12);try{var work=new ArrayList<Future<Boolean>>();for(long i=1;i<=60;i++){long user=i;work.add(pool.submit(()->{try{var ticket=gate.reserve(9L,user);gate.finish(ticket,true);return true;}catch(BusinessException e){return false;}}));}
  int successes=0;for(var f:work)if(f.get())successes++;assertThat(successes).isEqualTo(10);assertThat(redis.opsForHash().get(prefix+"{9}","stock")).isEqualTo("0");}finally{pool.shutdownNow();}
 }
 @Test void duplicateAndRollbackAreOwnerBoundAndIdempotent(){
  var first=gate.reserve(9L,7L);assertThatThrownBy(()->gate.reserve(9L,7L)).hasMessageContaining("限领");gate.finish(first,false);gate.finish(first,false);
  assertThat(redis.opsForHash().get(prefix+"{9}","stock")).isEqualTo("10");var next=gate.reserve(9L,7L);gate.finish(next,true);gate.finish(next,false);
  assertThat(redis.opsForHash().get(prefix+"{9}","stock")).isEqualTo("9");
 }
 @Test void oldReservationCannotRefundARebuiltSnapshot(){var old=gate.reserve(9L,7L);redis.delete(prefix+"{9}");var fresh=gate.reserve(9L,8L);gate.finish(old,false);assertThat(redis.opsForHash().get(prefix+"{9}","stock")).isEqualTo("9");gate.finish(fresh,true);}
 @Test void loadsHistoricalLimitsAndUsesRedisTimeForWindows(){when(users.countClaims(7L,9L)).thenReturn(1);assertThatThrownBy(()->gate.reserve(9L,7L)).hasMessageContaining("限领");redis.delete(prefix+"{9}");coupon.setStartsAt(LocalDateTime.now().plusDays(1));coupon.setEndsAt(LocalDateTime.now().plusDays(2));assertThatThrownBy(()->gate.reserve(9L,8L)).hasMessageContaining("开始");redis.delete(prefix+"{9}");coupon.setStartsAt(LocalDateTime.now().minusDays(2));coupon.setEndsAt(LocalDateTime.now().minusDays(1));assertThatThrownBy(()->gate.reserve(9L,8L)).hasMessageContaining("结束");}
 @Test void corruptStockDoesNotPartiallyIncrementDuringCompensation(){var ticket=gate.reserve(9L,7L);redis.opsForHash().put(prefix+"{9}","stock","bad");gate.finish(ticket,false);assertThat(redis.opsForHash().get(prefix+"{9}","u:7")).isEqualTo("1");redis.opsForHash().put(prefix+"{9}","stock","9");gate.finish(ticket,false);assertThat(redis.opsForHash().get(prefix+"{9}","stock")).isEqualTo("10");}
 @Test void redisFailureRejectsSpecialClaimsButKeepsNormalClaimsOnDatabasePath(){
  var offline=mock(StringRedisTemplate.class);when(offline.execute(any(org.springframework.data.redis.core.script.RedisScript.class),anyList(),any(Object[].class))).thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("offline"));
  var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenAnswer(i->new SimpleTransactionStatus());var unavailable=new CouponClaimGate(offline,coupons,users,manager,Clock.systemDefaultZone());
  assertThatThrownBy(()->unavailable.reserve(9L,7L)).hasMessageContaining("服务暂不可用");coupon.setClaimMode("NORMAL");assertThat(unavailable.reserve(9L,7L)).isNull();
 }

 @Test void unlimitedPersonalClaimsCanRepeatButStillStopAtTotalStock(){coupon.setPerUserLimit(0);for(int i=0;i<10;i++){var ticket=gate.reserve(9L,7L);gate.finish(ticket,true);}assertThatThrownBy(()->gate.reserve(9L,7L)).hasMessageContaining("抢光");assertThat(redis.opsForHash().get(prefix+"{9}","stock")).isEqualTo("0");}

}
