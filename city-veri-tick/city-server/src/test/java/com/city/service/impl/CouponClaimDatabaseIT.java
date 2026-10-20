package com.city.service.impl;

import com.city.dto.CreateCouponDTO;
import com.city.exception.BusinessException;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.city.cache.CouponClaimGate;
import com.city.mapper.*;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Actual MySQL + Redis, isolated ids and keys; cleans only the committed fixtures created here. */
class CouponClaimDatabaseIT {
 JdbcTemplate jdbc;TransactionTemplate tx;CouponServiceImpl service;CouponClaimGate gate;StringRedisTemplate redis;LettuceConnectionFactory redisFactory;
 com.zaxxer.hikari.HikariDataSource pool;long base;String prefix;final java.util.Set<Long> extraCoupons=new java.util.HashSet<>();Clock clock=Clock.system(ZoneId.of("Asia/Shanghai"));
 @BeforeEach void setup() throws Exception {
  var yaml=new YamlPropertiesFactoryBean();yaml.setResources(new ClassPathResource("application-dev.yml"));var p=Objects.requireNonNull(yaml.getObject());String k="city.datasource.";
  assertThat(p.getProperty(k+"host")).isIn("localhost","127.0.0.1");
  var ds=new DriverManagerDataSource("jdbc:mysql://"+p.getProperty(k+"host")+":"+p.getProperty(k+"port")+"/"+p.getProperty(k+"database")+"?serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",p.getProperty(k+"username"),p.getProperty(k+"password"));
  pool=new com.zaxxer.hikari.HikariDataSource();pool.setDataSource(ds);pool.setMaximumPoolSize(2);pool.setMinimumIdle(0);pool.setConnectionTimeout(3000);jdbc=new JdbcTemplate(pool);var manager=new DataSourceTransactionManager(pool);tx=new TransactionTemplate(manager);
  var cfg=new MybatisConfiguration();cfg.setMapUnderscoreToCamelCase(true);var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(pool);factory.setConfiguration(cfg);var sf=factory.getObject();for(Class<?> mapper:List.of(CouponMapper.class,UserCouponMapper.class,CouponCategoryScopeMapper.class))cfg.addMapper(mapper);var sql=new SqlSessionTemplate(sf);
  var rc=new RedisStandaloneConfiguration(p.getProperty("city.redis.host"),Integer.parseInt(p.getProperty("city.redis.port")));rc.setPassword(p.getProperty("city.redis.password"));rc.setDatabase(1);redisFactory=new LettuceConnectionFactory(rc,LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(4)).shutdownTimeout(Duration.ZERO).build());redisFactory.afterPropertiesSet();redis=new StringRedisTemplate(redisFactory);redis.afterPropertiesSet();prefix="test:coupon-db:"+UUID.randomUUID()+":";
  gate=new CouponClaimGate(redis,sql.getMapper(CouponMapper.class),sql.getMapper(UserCouponMapper.class),manager,clock);ReflectionTestUtils.setField(gate,"prefix",prefix);
  service=new CouponServiceImpl();ReflectionTestUtils.setField(service,"transactionManager",manager);ReflectionTestUtils.setField(service,"coupons",sql.getMapper(CouponMapper.class));ReflectionTestUtils.setField(service,"userCoupons",sql.getMapper(UserCouponMapper.class));ReflectionTestUtils.setField(service,"scopes",sql.getMapper(CouponCategoryScopeMapper.class));ReflectionTestUtils.setField(service,"claimGate",gate);ReflectionTestUtils.setField(service,"clock",clock);
  base=Math.abs(UUID.randomUUID().getLeastSignificantBits()%1000000000L)+2000000000000L;
  for(int i=0;i<24;i++)jdbc.update("INSERT INTO city_user(id,username,password_hash) VALUES(?,?,?)",base+i,"claim_it_"+base+"_"+i,"unused");
  jdbc.update("INSERT INTO coupon(id,name,coupon_type,claim_mode,discount_amount,min_order_amount,total_quantity,claimed_quantity,per_user_limit,status,starts_at,ends_at,use_starts_at,use_ends_at) VALUES(?,?,'NO_THRESHOLD','FLASH_SALE',5,0,10,0,1,'ACTIVE',?,?,?,?)",base,"领取集成测试",LocalDateTime.now(clock).minusDays(1),LocalDateTime.now(clock).plusDays(1),LocalDateTime.now(clock).minusDays(1),LocalDateTime.now(clock).plusDays(2));
 }
 @AfterEach void cleanup(){try{if(jdbc!=null&&base>0){for(Long id:extraCoupons){jdbc.update("DELETE FROM user_coupon WHERE coupon_id=?",id);jdbc.update("DELETE FROM coupon WHERE id=?",id);}jdbc.update("DELETE FROM user_coupon WHERE coupon_id=?",base);jdbc.update("DELETE FROM coupon WHERE id=?",base);jdbc.update("DELETE FROM city_user WHERE id BETWEEN ? AND ? AND username LIKE ?",base,base+23,"claim_it_"+base+"_%");}if(redis!=null){var keys=redis.keys(prefix+"*");if(keys!=null&&!keys.isEmpty())redis.delete(keys);}}finally{if(redisFactory!=null)redisFactory.destroy();if(pool!=null)pool.close();}}
 @Test void onlyCommittedCouponsAreSuccessfulAndDatabaseNeverOversells() throws Exception {
  var pool=Executors.newFixedThreadPool(8);try{var jobs=new ArrayList<Future<Boolean>>();for(int i=0;i<24;i++){long user=base+i;jobs.add(pool.submit(()->{try{service.claim(user,base);return true;}catch(BusinessException e){return false;}}));}
  int success=0;for(var job:jobs)if(job.get(30,TimeUnit.SECONDS))success++;
  assertThat(success).isEqualTo(10);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_coupon WHERE coupon_id=?",Integer.class,base)).isEqualTo(10);
  assertThat(jdbc.queryForObject("SELECT claimed_quantity FROM coupon WHERE id=?",Integer.class,base)).isEqualTo(10);
  assertThat(redis.opsForHash().get(prefix+"{"+base+"}","stock")).isEqualTo("0");
  assertThat(redis.opsForHash().keys(prefix+"{"+base+"}")).noneMatch(v->v.toString().startsWith("r:"));
  }finally{pool.shutdownNow();}
 }
 @Test void transactionRollbackReturnsReservationAndRestartRebuildsPersonalLimits(){
  assertThatThrownBy(()->service.claim(base+100,base)).isInstanceOf(org.springframework.dao.DataAccessException.class);
  assertThat(jdbc.queryForObject("SELECT claimed_quantity FROM coupon WHERE id=?",Integer.class,base)).isZero();assertThat(redis.opsForHash().get(prefix+"{"+base+"}","stock")).isEqualTo("10");
  service.claim(base,base);
  redis.delete(prefix+"{"+base+"}");
  assertThatThrownBy(()->tx.execute(status->service.claim(base,base))).hasMessageContaining("限领");
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_coupon WHERE coupon_id=?",Integer.class,base)).isEqualTo(1);
  tx.executeWithoutResult(status->service.setStatus(base,"INACTIVE"));
  assertThatThrownBy(()->tx.execute(status->service.claim(base+1,base))).hasMessageContaining("不可用");
  tx.executeWithoutResult(status->service.setStatus(base,"ACTIVE"));service.claim(base+1,base);
  assertThat(jdbc.queryForObject("SELECT claimed_quantity FROM coupon WHERE id=?",Integer.class,base)).isEqualTo(2);
 }
 @Test void unlimitedPersonalClaimsStillConsumeRealStockAfterRedisRebuild(){
  jdbc.update("UPDATE coupon SET per_user_limit=0,total_quantity=3 WHERE id=?",base);
  service.claim(base,base);redis.delete(prefix+"{"+base+"}");service.claim(base,base);service.claim(base,base);
  assertThatThrownBy(()->service.claim(base,base)).hasMessageContaining("抢光");
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_coupon WHERE coupon_id=? AND user_id=?",Integer.class,base,base)).isEqualTo(3);
  assertThat(jdbc.queryForObject("SELECT claimed_quantity FROM coupon WHERE id=?",Integer.class,base)).isEqualTo(3);
 }

 @Test void newlyCreatedCouponRequiresExplicitActivationBeforeAnyClaim(){
  for(String mode:java.util.List.of("NORMAL","LIMITED","FLASH_SALE")){
  var d=new CreateCouponDTO();d.setName("未投放券");d.setClaimMode(mode);d.setCouponType("NO_THRESHOLD");d.setDiscountAmount(new java.math.BigDecimal("5"));d.setTotalQuantity(10);d.setStartsAt(LocalDateTime.now(clock).minusDays(1));d.setEndsAt(LocalDateTime.now(clock).plusDays(1));d.setUseStartsAt(d.getStartsAt());d.setUseEndsAt(d.getEndsAt().plusDays(1));
  var created=tx.execute(status->service.create(d));extraCoupons.add(created.getId());assertThat(created.getStatus()).isEqualTo("DRAFT");assertThat(service.available(base)).noneMatch(c->c.getId().equals(created.getId()));assertThat(redis.hasKey(prefix+"{"+created.getId()+"}")).isFalse();
  assertThatThrownBy(()->service.claim(base,created.getId())).isInstanceOf(BusinessException.class);assertThat(jdbc.queryForObject("SELECT claimed_quantity FROM coupon WHERE id=?",Integer.class,created.getId())).isZero();
  tx.executeWithoutResult(status->service.setStatus(created.getId(),"ACTIVE"));service.claim(base,created.getId());assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_coupon WHERE coupon_id=?",Integer.class,created.getId())).isEqualTo(1);
  }
 }

}
