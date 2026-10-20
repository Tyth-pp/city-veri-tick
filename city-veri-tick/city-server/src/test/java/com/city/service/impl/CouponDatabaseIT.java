package com.city.service.impl;

import com.city.cache.CouponClaimGate;
import com.city.exception.BusinessException;
import com.city.service.FileStorageService;
import com.city.service.RefundService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.city.dto.CreateCouponDTO;
import com.city.entity.Coupon;
import com.city.mapper.*;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Local MySQL regression; every fixture and business write rolls back. */
class CouponDatabaseIT {
    @Test void actualMappingsApplyLimitsScopePaymentAndRefundPolicies() throws Exception {
        var yaml=new YamlPropertiesFactoryBean(); yaml.setResources(new ClassPathResource("application-dev.yml"));
        Properties p=yaml.getObject(); String key="city.datasource.";
        assertThat(p.getProperty(key+"host")).isIn("localhost","127.0.0.1");
        var ds=new DriverManagerDataSource("jdbc:mysql://"+p.getProperty(key+"host")+":"+p.getProperty(key+"port")+"/"+p.getProperty(key+"database")+"?serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",p.getProperty(key+"username"),p.getProperty(key+"password"));
        var config=new MybatisConfiguration(); config.setMapUnderscoreToCamelCase(true);
        var factory=new MybatisSqlSessionFactoryBean(); factory.setDataSource(ds);factory.setConfiguration(config);
        var sqlFactory=factory.getObject();
        for(Class<?> mapper:List.of(CouponMapper.class,UserCouponMapper.class,CouponCategoryScopeMapper.class,ActivityMapper.class,ActivitySessionMapper.class,TicketTypeMapper.class,TicketOrderMapper.class,TicketMapper.class))config.addMapper(mapper);
        var sql=new SqlSessionTemplate(sqlFactory); var jdbc=new JdbcTemplate(ds);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        Long before=jdbc.queryForObject("SELECT COUNT(*) FROM coupon",Long.class);
        long base=Math.abs(UUID.randomUUID().getLeastSignificantBits()%1000000000L)+1000000000000L;
        String suffix=UUID.randomUUID().toString().replace("-","");
        tx.executeWithoutResult(status->{
            try{
                jdbc.update("INSERT INTO city_user(id,username,password_hash,staff_name) VALUES(?,?,?,?)",base,"coupon_it_"+suffix,"unused","测试用户");
                jdbc.update("INSERT INTO organizer(id,name,contact_name,contact_phone,status) VALUES(?,?,?,?,?)",base,"优惠券测试机构","测试","00000000000","APPROVED");
                jdbc.update("INSERT INTO activity(id,organizer_id,title,status,category) VALUES(?,?,?,?,?)",base,base,"优惠券测试活动","PUBLISHED","CONCERT");
                jdbc.update("INSERT INTO activity_session(id,activity_id,start_time,end_time,capacity,status) VALUES(?,?,?, ?,100,'ON_SALE')",base,base,LocalDateTime.of(2099,1,1,19,0),LocalDateTime.of(2099,1,1,21,0));
                jdbc.update("INSERT INTO ticket_type(id,session_id,name,price,stock,locked_stock,enabled) VALUES(?,?,?,1000,10,0,1)",base,base,"测试票种");
                for(int i=0;i<2;i++) jdbc.update("INSERT INTO ticket_order(id,order_no,user_id,ticket_type_id,quantity,amount,pay_amount,status) VALUES(?,?,?,?,1,1000,900,'PENDING_PAYMENT')",base-i,"CP"+suffix.substring(0,28)+i,base,base);
                var service=new CouponServiceImpl(); ReflectionTestUtils.setField(service,"transactionManager",tx.getTransactionManager());
                ReflectionTestUtils.setField(service,"claimGate",org.mockito.Mockito.mock(CouponClaimGate.class)); ReflectionTestUtils.setField(service,"coupons",sql.getMapper(CouponMapper.class)); ReflectionTestUtils.setField(service,"scopes",sql.getMapper(CouponCategoryScopeMapper.class));
                ReflectionTestUtils.setField(service,"userCoupons",sql.getMapper(UserCouponMapper.class)); ReflectionTestUtils.setField(service,"activities",sql.getMapper(ActivityMapper.class));
                ReflectionTestUtils.setField(service,"sessions",sql.getMapper(ActivitySessionMapper.class)); ReflectionTestUtils.setField(service,"types",sql.getMapper(TicketTypeMapper.class));
                ReflectionTestUtils.setField(service,"orders",sql.getMapper(TicketOrderMapper.class));
                ReflectionTestUtils.setField(service,"clock",Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"),ZoneOffset.UTC));
                CreateCouponDTO d=new CreateCouponDTO();d.setName("折扣集成测试");d.setCouponType("DISCOUNT");d.setDiscountRate(new BigDecimal("0.85"));
                d.setMaxDiscountAmount(new BigDecimal("100"));d.setTotalQuantity(10);d.setPerUserLimit(2);d.setCategoryCodes(List.of("CONCERT"));
                d.setStartsAt(LocalDateTime.of(2029,1,1,0,0));d.setEndsAt(LocalDateTime.of(2030,1,2,0,0));d.setUseStartsAt(d.getStartsAt());d.setUseEndsAt(LocalDateTime.of(2031,1,1,0,0));
                jdbc.update("UPDATE ticket_order SET expired_at=? WHERE id IN (?,?)",LocalDateTime.of(2030,1,1,0,15),base,base-1);
                Coupon c=service.create(d); service.setStatus(c.getId(),"ACTIVE");
                service.claim(base,c.getId());service.claim(base,c.getId());
                assertThat(service.mine(base).getUsableCount()).isEqualTo(2);
                assertThat(service.mine(base-1000).getItems()).isEmpty();
                assertThatThrownBy(()->service.claim(base,c.getId())).hasMessageContaining("已领取");
                assertThat(jdbc.queryForObject("SELECT claimed_quantity FROM coupon WHERE id=?",Integer.class,c.getId())).isEqualTo(2);
                assertThat(service.choices(base,base,1)).singleElement().satisfies(o->{assertThat(o.isUsable()).isTrue();assertThat(o.getDiscountAmount()).isEqualByComparingTo("100");assertThat(o.getOwnedQuantity()).isEqualTo(2);});
                assertThat(service.lockForOrder(base,c.getId(),base,new BigDecimal("1000"),"CONCERT",base)).isEqualByComparingTo("100");
                service.lockForOrder(base,c.getId(),base-1,new BigDecimal("1000"),"CONCERT",base);
                assertThat(service.mine(base).getUsableCount()).isZero();
                assertThat(service.mine(base).getItems()).allMatch(v -> "LOCKED".equals(v.getState()) && v.isCanPay());
                service.use(base,c.getId(),base);service.release(base,c.getId(),base-1);
                assertThat(service.mine(base).getUsedCount()).isEqualTo(1);assertThat(service.mine(base).getUsableCount()).isEqualTo(1);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_coupon WHERE coupon_id=? AND status='AVAILABLE'",Integer.class,c.getId())).isEqualTo(1);
                service.restoreAfterRefund(base,c.getId(),base);
                assertThat(service.mine(base).getUsableCount()).isEqualTo(2);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_coupon WHERE coupon_id=? AND status='AVAILABLE'",Integer.class,c.getId())).isEqualTo(2);
                // Only the coupon belonging to this order is consumed; no-return keeps it used.
                service.lockForOrder(base,c.getId(),base,new BigDecimal("1000"),"CONCERT",base); service.use(base,c.getId(),base);
                jdbc.update("UPDATE coupon SET refund_policy='NO_RETURN' WHERE id=?",c.getId());
                service.restoreAfterRefund(base,c.getId(),base);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_coupon WHERE coupon_id=? AND status='USED'",Integer.class,c.getId())).isEqualTo(1);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_coupon WHERE coupon_id=? AND status='AVAILABLE'",Integer.class,c.getId())).isEqualTo(1);
                // Activity scope takes precedence; no category rows are persisted.
                d.setName("指定活动测试");d.setScopeType("ACTIVITY");d.setClaimMode("FLASH_SALE");
                jdbc.update("INSERT INTO activity(id,organizer_id,title,status,category) VALUES(?,?,?,?,?)",base-2,base,"指定活动测试","PUBLISHED","MOVIE");
                d.setActivityId(base-2);
                Coupon specific=service.create(d); service.setStatus(specific.getId(),"ACTIVE"); assertThat(jdbc.queryForObject("SELECT claim_mode FROM coupon WHERE id=?",String.class,specific.getId())).isEqualTo("FLASH_SALE"); service.claim(base,specific.getId());
                assertThat(specific.getCategoryCodes()).isEmpty();
                assertThatThrownBy(()->service.lockForOrder(base,specific.getId(),base-1,new BigDecimal("1000"),"CONCERT",base)).hasMessageContaining("指定活动");
                // Timer expiry uses a real conditional close, returns locked stock/coupon exactly once.
                jdbc.update("INSERT INTO ticket_order(id,order_no,user_id,ticket_type_id,quantity,amount,pay_amount,discount_amount,coupon_id,status,expired_at) VALUES(?,?,?,?,1,1000,900,100,?,'PENDING_PAYMENT',?)",base-3,"CE"+suffix.substring(0,28),base,base,c.getId(),LocalDateTime.of(2030,1,1,0,0));
                assertThat(sql.getMapper(TicketTypeMapper.class).lockStock(base,1)).isEqualTo(1);
                service.lockForOrder(base,c.getId(),base-3,new BigDecimal("1000"),"CONCERT",base);
                var orderService=new OrderServiceImpl();
                ReflectionTestUtils.setField(orderService,"ticketOrderMapper",sql.getMapper(TicketOrderMapper.class));
                ReflectionTestUtils.setField(orderService,"ticketTypeMapper",sql.getMapper(TicketTypeMapper.class));
                ReflectionTestUtils.setField(orderService,"ticketMapper",sql.getMapper(TicketMapper.class));
                ReflectionTestUtils.setField(orderService,"activitySessionMapper",sql.getMapper(ActivitySessionMapper.class));
                ReflectionTestUtils.setField(orderService,"activityMapper",sql.getMapper(ActivityMapper.class));
                ReflectionTestUtils.setField(orderService,"couponMapper",sql.getMapper(CouponMapper.class));
                ReflectionTestUtils.setField(orderService,"couponService",service);
                ReflectionTestUtils.setField(orderService,"fileStorageService",org.mockito.Mockito.mock(FileStorageService.class));
                ReflectionTestUtils.setField(orderService,"refundService",org.mockito.Mockito.mock(RefundService.class));
                ReflectionTestUtils.setField(orderService,"clock",Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"),ZoneOffset.UTC));
                assertThat(orderService.synchronizeExpiry(base,base-3).getStatus()).isEqualTo("EXPIRED");
                assertThat(orderService.synchronizeExpiry(base,base-3).getStatus()).isEqualTo("EXPIRED");
                assertThat(jdbc.queryForObject("SELECT stock FROM ticket_type WHERE id=?",Integer.class,base)).isEqualTo(10);
                assertThat(jdbc.queryForObject("SELECT locked_stock FROM ticket_type WHERE id=?",Integer.class,base)).isZero();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_coupon WHERE coupon_id=? AND status='AVAILABLE'",Integer.class,c.getId())).isEqualTo(1);
                d.setName("不封顶不限个人次数");d.setClaimMode("NORMAL");d.setScopeType("ALL");d.setActivityId(null);d.setMaxDiscountAmount(null);d.setPerUserLimit(0);d.setTotalQuantity(3);
                Coupon unlimited=service.create(d); service.setStatus(unlimited.getId(),"ACTIVE");service.claim(base,unlimited.getId());service.claim(base,unlimited.getId());service.claim(base,unlimited.getId());
                assertThat(jdbc.queryForObject("SELECT max_discount_amount FROM coupon WHERE id=?",BigDecimal.class,unlimited.getId())).isNull();
                assertThat(jdbc.queryForObject("SELECT per_user_limit FROM coupon WHERE id=?",Integer.class,unlimited.getId())).isZero();
                assertThatThrownBy(()->service.claim(base,unlimited.getId())).isInstanceOf(BusinessException.class);
            }finally{status.setRollbackOnly();}
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM coupon",Long.class)).isEqualTo(before);
    }
}
