package com.city.service.impl;
import com.city.cache.ActivityListCache;
import com.city.cache.PublicActivityCache;
import com.city.vo.OrderViewVO;
import java.util.List;
import java.util.Map;
import com.city.security.LoginPrincipal;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.city.entity.*;
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
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Explicit local integration check. All fixtures and business writes are rolled back. */
class RefundDatabaseIT {
    @Test void actualMysqlMappingsCompleteApplyRejectReapplyAndRefund() throws Exception {
        var yaml=new YamlPropertiesFactoryBean();yaml.setResources(new ClassPathResource("application-dev.yml"));
        Properties p=yaml.getObject();String prefix="city.datasource.";
        assertThat(p.getProperty(prefix+"host")).isIn("localhost","127.0.0.1");
        var ds=new DriverManagerDataSource("jdbc:mysql://"+p.getProperty(prefix+"host")+":"+p.getProperty(prefix+"port")+"/"+p.getProperty(prefix+"database")+"?serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",p.getProperty(prefix+"username"),p.getProperty(prefix+"password"));
        var configuration=new MybatisConfiguration();configuration.setMapUnderscoreToCamelCase(true);
        var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(configuration);
        var sqlFactory=factory.getObject();
        for(Class<?> mapper:List.of(TicketOrderMapper.class,TicketMapper.class,TicketTypeMapper.class,ActivitySessionMapper.class,ActivityMapper.class,RefundRequestMapper.class,UserMapper.class,OrganizerMemberMapper.class,ManagementMessageMapper.class,ManagementDashboardMapper.class))configuration.addMapper(mapper);
        var sql=new SqlSessionTemplate(sqlFactory);var jdbc=new JdbcTemplate(ds);var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        Long before=jdbc.queryForObject("SELECT COUNT(*) FROM refund_request",Long.class);
        Long messagesBefore=jdbc.queryForObject("SELECT COUNT(*) FROM management_message",Long.class);
        long base=-Math.abs(UUID.randomUUID().getLeastSignificantBits()%1000000000L)-10000;
        String suffix=UUID.randomUUID().toString();
        tx.executeWithoutResult(status->{
            try{
                jdbc.update("INSERT INTO city_user(id,username,password_hash,staff_name) VALUES(?,?,?,?)",base,"refund_test_buyer_"+suffix,"unused","集成测试买家");
                jdbc.update("INSERT INTO city_user(id,username,password_hash,staff_name) VALUES(?,?,?,?)",base-1,"refund_test_reviewer_"+suffix,"unused","集成审核人");
                jdbc.update("INSERT INTO organizer(id,name,contact_name,contact_phone,status) VALUES(?,?,?,?,?)",base,"退款集成测试机构","测试","00000000000","APPROVED");
                jdbc.update("INSERT INTO organizer_member(id,organizer_id,user_id,member_role,enabled) VALUES(?,?,?,?,1)",base,base,base-1,"OWNER");
                jdbc.update("INSERT INTO activity(id,organizer_id,title,status) VALUES(?,?,?,?)",base,base,"退款集成测试活动","PUBLISHED");
                jdbc.update("INSERT INTO activity_session(id,activity_id,start_time,end_time,capacity,status) VALUES(?,?,?, ?,100,'ON_SALE')",base,base,LocalDateTime.of(2099,1,1,19,0),LocalDateTime.of(2099,1,1,21,0));
                jdbc.update("INSERT INTO ticket_type(id,session_id,name,price,stock,locked_stock,enabled) VALUES(?,?,?,98,9,0,1)",base,base,"测试票种");
                jdbc.update("INSERT INTO ticket_order(id,order_no,user_id,ticket_type_id,quantity,amount,pay_amount,status) VALUES(?,?,?,?,1,98,88,'PAID')",base,"IT"+suffix.replace("-","").substring(0,28),base,base);
                jdbc.update("INSERT INTO ticket(id,ticket_no,order_id,ticket_type_id,verification_code,status) VALUES(?,?,?,?,?,'UNUSED')",base,"IT"+suffix.replace("-","").substring(0,28),base,base,"IT"+suffix);
                var access=new OrganizerAccessServiceImpl();ReflectionTestUtils.setField(access,"organizerMemberMapper",sql.getMapper(OrganizerMemberMapper.class));
                var service=new RefundServiceImpl();
                ReflectionTestUtils.setField(service,"orders",sql.getMapper(TicketOrderMapper.class));ReflectionTestUtils.setField(service,"tickets",sql.getMapper(TicketMapper.class));
                ReflectionTestUtils.setField(service,"types",sql.getMapper(TicketTypeMapper.class));ReflectionTestUtils.setField(service,"sessions",sql.getMapper(ActivitySessionMapper.class));
                ReflectionTestUtils.setField(service,"activities",sql.getMapper(ActivityMapper.class));ReflectionTestUtils.setField(service,"refunds",sql.getMapper(RefundRequestMapper.class));
                ReflectionTestUtils.setField(service,"users",sql.getMapper(UserMapper.class));ReflectionTestUtils.setField(service,"access",access);ReflectionTestUtils.setField(service,"clock",Clock.system(ZoneId.of("Asia/Shanghai")));
                ReflectionTestUtils.setField(service,"messages",new ManagementMessageServiceImpl(sql.getMapper(ManagementMessageMapper.class),access,Clock.system(ZoneId.of("Asia/Shanghai"))));
                var first=service.apply(base,base,"行程变化");
                var messages=new ManagementMessageServiceImpl(sql.getMapper(ManagementMessageMapper.class),access,Clock.system(ZoneId.of("Asia/Shanghai")));
                var actor=new LoginPrincipal(base-1,"ORGANIZER_ADMIN","it",Instant.now(),Instant.now());
                assertThat(((Number)messages.inbox(actor,1).get("unreadCount")).longValue()).isEqualTo(1);
                var unread=(List<Map<String,Object>>)messages.inbox(actor,1).get("unread");
                long messageId=((Number)unread.get(0).get("id")).longValue();messages.read(actor,messageId);
                assertThat(((Number)messages.inbox(actor,1).get("unreadCount")).longValue()).isZero();
                messages.publish("ORGANIZER_ADMIN",base-2,"TEST","其他机构","权限隔离测试","/management/organizer/orders/1");
                assertThat((List<?>)messages.inbox(actor,1).get("items")).hasSize(1);
                assertThat(jdbc.queryForObject("SELECT status FROM ticket_order WHERE id=?",String.class,base)).isEqualTo("REFUNDING");
                service.review(base-1,base,first.getId(),false,"请补充说明");
                assertThat(jdbc.queryForObject("SELECT status FROM ticket_order WHERE id=?",String.class,base)).isEqualTo("PAID");
                assertThat(jdbc.queryForObject("SELECT stock FROM ticket_type WHERE id=?",Integer.class,base)).isEqualTo(9);
                var second=service.apply(base,base,"重新说明行程冲突");
                var done=service.review(base-1,base,second.getId(),true,"核对通过");
                assertThat(done.getAmount()).isEqualByComparingTo("88");assertThat(done.getReviewerName()).isEqualTo("集成审核人");
                assertThat(jdbc.queryForObject("SELECT status FROM ticket_order WHERE id=?",String.class,base)).isEqualTo("REFUNDED");
                assertThat(jdbc.queryForObject("SELECT status FROM ticket WHERE id=?",String.class,base)).isEqualTo("REFUNDED");
                assertThat(jdbc.queryForObject("SELECT stock FROM ticket_type WHERE id=?",Integer.class,base)).isEqualTo(10);
                assertThatThrownBy(()->service.review(base-1,base,second.getId(),true,"重复操作")).hasMessageContaining("已处理");
                assertThat(jdbc.queryForObject("SELECT stock FROM ticket_type WHERE id=?",Integer.class,base)).isEqualTo(10);
                var view=new OrderViewVO();service.enrich(view,sql.getMapper(TicketOrderMapper.class).selectById(base),true);
                assertThat(view.getRefundHistory()).hasSize(2);assertThat(view.getRefundEligible()).isFalse();
                jdbc.update("UPDATE ticket_order SET paid_at=NOW() WHERE id=?",base);
                var dashboard=new ManagementDashboardServiceImpl(sql.getMapper(ManagementDashboardMapper.class),Clock.system(ZoneId.of("Asia/Shanghai")));
                var stats=dashboard.summary(base,7);
                assertThat(new java.math.BigDecimal(stats.get("paidRevenue").toString())).isEqualByComparingTo("88");
                assertThat(new java.math.BigDecimal(stats.get("refundAmount").toString())).isEqualByComparingTo("88");
                assertThat(new java.math.BigDecimal(stats.get("netAmount").toString())).isEqualByComparingTo("0");
                assertThat(((Number)stats.get("pendingUseTickets")).longValue()).isZero();
                assertThat((List<?>)stats.get("trend")).hasSize(7);
                dashboard.summary(null,30);
                // Exercise actual submit -> reject -> resubmit -> approve -> offline transitions.
                jdbc.update("UPDATE activity SET status='DRAFT' WHERE id=?",base);
                sql.clearCache(); // Fixture JDBC changes must invalidate MyBatis' transaction-local read cache.
                var organizerService=new OrganizerActivityServiceImpl();
                ReflectionTestUtils.setField(organizerService,"publicActivityCache",org.mockito.Mockito.mock(PublicActivityCache.class));
                ReflectionTestUtils.setField(organizerService,"activityListCache",org.mockito.Mockito.mock(ActivityListCache.class));
                ReflectionTestUtils.setField(organizerService,"activityMapper",sql.getMapper(ActivityMapper.class));ReflectionTestUtils.setField(organizerService,"activitySessionMapper",sql.getMapper(ActivitySessionMapper.class));
                ReflectionTestUtils.setField(organizerService,"ticketTypeMapper",sql.getMapper(TicketTypeMapper.class));ReflectionTestUtils.setField(organizerService,"organizerAccessService",access);ReflectionTestUtils.setField(organizerService,"messages",messages);
                organizerService.submit(base-1,base);
                var platformService=new PlatformActivityServiceImpl();
                ReflectionTestUtils.setField(platformService,"publicActivityCache",org.mockito.Mockito.mock(PublicActivityCache.class));
                ReflectionTestUtils.setField(platformService,"activityListCache",org.mockito.Mockito.mock(ActivityListCache.class));
                ReflectionTestUtils.setField(platformService,"activityMapper",sql.getMapper(ActivityMapper.class));ReflectionTestUtils.setField(platformService,"activitySessionMapper",sql.getMapper(ActivitySessionMapper.class));ReflectionTestUtils.setField(platformService,"messages",messages);
                platformService.reject(base-1,base,"请补充资料");organizerService.submit(base-1,base);platformService.approve(base-1,base);
                assertThat(sql.getMapper(ActivityMapper.class).isPublished(base)).isTrue();
                assertThatThrownBy(()->platformService.approve(base-1,base)).isInstanceOf(RuntimeException.class);
                platformService.offline(base-1,base,"场地安排变更");
                assertThat(sql.getMapper(ActivityMapper.class).isPublished(base)).isFalse();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM management_message WHERE audience='PLATFORM_ADMIN' AND target_path=?",Long.class,"/management/platform/activities/"+base)).isEqualTo(2);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM management_message WHERE event_type='ACTIVITY_OFFLINE' AND organizer_id=? AND content LIKE '%场地安排变更%'",Long.class,base)).isEqualTo(1);
            }finally{status.setRollbackOnly();}
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refund_request",Long.class)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM management_message",Long.class)).isEqualTo(messagesBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM city_user WHERE id IN (?,?)",Long.class,base,base-1)).isZero();
    }
}
