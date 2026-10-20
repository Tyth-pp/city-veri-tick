package com.city.acceptance;

import com.city.CityApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.redis.core.ScanOptions;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

/** Opt-in real HTTP + local MySQL/Redis acceptance; removes only this run's fixtures. */
@SpringBootTest(classes=CityApplication.class, webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties={"server.port=18082","spring.main.banner-mode=off",
        "city.jwt.blacklist-prefix=acceptance:blacklist:","city.jwt.user-tokens-prefix=acceptance:sessions:"})
@Import(FourRoleAcceptanceIT.NoScheduledJobs.class)
class FourRoleAcceptanceIT {
    static final String REDIS_PREFIX="acceptance:"+UUID.randomUUID()+":";
    @DynamicPropertySource static void isolatedRedis(DynamicPropertyRegistry r) {
        r.add("city.jwt.blacklist-prefix",()->REDIS_PREFIX+"blacklist:");
        r.add("city.jwt.user-tokens-prefix",()->REDIS_PREFIX+"sessions:");
        r.add("city.cache.activity-prefix",()->REDIS_PREFIX+"activity:");
        r.add("city.cache.activity-list-prefix",()->REDIS_PREFIX+"list:");
        r.add("city.coupon.claim-prefix",()->REDIS_PREFIX+"coupon:");
    }
    @TestConfiguration static class NoScheduledJobs {
        @Bean static BeanFactoryPostProcessor disableBackgroundJobs() {
            return factory->{
                var registry=(BeanDefinitionRegistry)factory;
                String name="org.springframework.context.annotation.internalScheduledAnnotationProcessor";
                if(registry.containsBeanDefinition(name))registry.removeBeanDefinition(name);
            };
        }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired StringRedisTemplate redis;
    final HttpClient http=HttpClient.newHttpClient();
    final String prefix="e2e_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
    final String password=UUID.randomUUID()+"Aa1!";
    final long base=4000000000000L+Math.abs(UUID.randomUUID().getLeastSignificantBits()%1000000000);
    final Map<String,String> tokens=new HashMap<>();
    long activity,type,coupon;
    boolean ownsFixtures;
    int requests,negativeChecks;

    @Test void fourRolesCompleteRealBusinessAndBrowserJourneys() throws Exception {
        try {
            fixtures();
            for(String role:List.of("PLATFORM_ADMIN","ORGANIZER_ADMIN","USER","VERIFIER","OTHER_USER","OTHER_ORG")) {
                JsonNode login=ok(null,"POST","/auth/login",Map.of("username",prefix+role,"password",password));
                tokens.put(role,login.path("accessToken").asText());assertThat(tokens.get(role)).isNotBlank();
            }
            mark("真实账号登录：四角色及两个隔离账号");
            status(null,"GET","/user/orders",null,401);
            status("USER","GET","/admin/platform/activities",null,403);
            status("VERIFIER","GET","/user/orders",null,403);
            status("ORGANIZER_ADMIN","GET","/admin/platform/activities",null,403);
            status("PLATFORM_ADMIN","GET","/admin/organizer/activities",null,403);
            mark("未登录及角色越权拦截");
            String now=LocalDateTime.now().minusHours(1).withNano(0).toString();
            String future=LocalDateTime.now().plusDays(10).withNano(0).toString();
            JsonNode a=ok("ORGANIZER_ADMIN","POST","/admin/organizer/activities",Map.of(
                    "title",prefix+"城市活动验收","category","CONCERT","content","真实联调验收内容",
                    "highlights","四角色业务联动","notices","隔离测试活动","venueName","验收场地","venueAddress","验收地址"));
            activity=a.path("id").asLong();assertThat(activity).isPositive();
            status("OTHER_ORG","POST","/admin/organizer/activities/"+activity+"/submit",null,403);
            JsonNode session=ok("ORGANIZER_ADMIN","POST","/admin/organizer/activities/"+activity+"/sessions",Map.of(
                    "startTime",future,"endTime",LocalDateTime.now().plusDays(10).plusHours(2).withNano(0).toString(),
                    "capacity",30,"saleStartTime",now,"saleEndTime",future));
            JsonNode ticketType=ok("ORGANIZER_ADMIN","POST","/admin/organizer/sessions/"+session.path("id").asText()+"/ticket-types",
                    Map.of("name","验收票种","price",100,"stock",30,"enabled",true));
            type=ticketType.path("id").asLong();assertThat(type).isPositive();
            ok("ORGANIZER_ADMIN","POST","/admin/organizer/activities/"+activity+"/submit",null);
            assertThat(ok("PLATFORM_ADMIN","GET","/management/messages",null).path("items").toString()).contains(String.valueOf(activity));
            ok("PLATFORM_ADMIN","POST","/admin/platform/activities/"+activity+"/reject",Map.of("reason","补充资料后重提"));
            ok("ORGANIZER_ADMIN","POST","/admin/organizer/activities/"+activity+"/submit",null);
            ok("PLATFORM_ADMIN","POST","/admin/platform/activities/"+activity+"/approve",null);
            status("PLATFORM_ADMIN","POST","/admin/platform/activities/"+activity+"/approve",null,409);
            assertThat(ok(null,"GET","/user/activities/"+activity,null).toString()).contains(prefix);
            mark("主办方配置、平台驳回/重提/通过、公开查询与业务消息");
            JsonNode c=ok("PLATFORM_ADMIN","POST","/admin/platform/coupons",Map.ofEntries(
                    Map.entry("name",prefix+"验收券"),Map.entry("couponType","NO_THRESHOLD"),Map.entry("claimMode","LIMITED"),
                    Map.entry("discountAmount",10),Map.entry("totalQuantity",3),Map.entry("perUserLimit",1),
                    Map.entry("startsAt",now),Map.entry("endsAt",future),Map.entry("useStartsAt",now),Map.entry("useEndsAt",future)));
            coupon=c.path("id").asLong();assertThat(coupon).isPositive();
            status("USER","POST","/user/coupons/"+coupon+"/claim",null,409);
            ok("PLATFORM_ADMIN","PATCH","/admin/platform/coupons/"+coupon+"/status?value=ACTIVE",null);
            ok("USER","POST","/user/coupons/"+coupon+"/claim",null);
            status("USER","POST","/user/coupons/"+coupon+"/claim",null,409);
            assertThat(ok("USER","GET","/user/coupons/mine",null).toString()).contains(prefix+"验收券");
            mark("限量券创建/启用/领取/个人限领/个人券包");
            Map<String,Object> body=Map.of("ticketTypeId",type,"quantity",1,"couponId",coupon);
            JsonNode order=ok("USER","POST","/user/orders",body,"Idempotency-Key",prefix+"first");
            long id=order.path("orderId").asLong();assertThat(id).isPositive();
            assertThat(ok("USER","POST","/user/orders",body,"Idempotency-Key",prefix+"first").path("orderId").asLong()).isEqualTo(id);
            assertThat(order.path("payAmount").decimalValue()).isEqualByComparingTo("90");
            status("OTHER_USER","GET","/user/orders/"+id,null,404);
            status("OTHER_ORG","GET","/admin/organizer/orders/"+id,null,404);
            ok("USER","POST","/user/orders/"+id+"/cancel",null);stock(30,0);
            order=ok("USER","POST","/user/orders",body);id=order.path("orderId").asLong();
            jdbc.update("UPDATE ticket_order SET expired_at=? WHERE id=?",LocalDateTime.now().minusMinutes(1),id);
            status("USER","POST","/user/orders/"+id+"/pay",null,409);
            assertThat(ok("USER","POST","/user/orders/"+id+"/sync-expiry",null).path("status").asText()).isEqualTo("EXPIRED");
            ok("USER","POST","/user/orders/"+id+"/sync-expiry",null);stock(30,0);
            mark("订单请求幂等、归属隔离、取消/超时及优惠券/库存释放");
            order=ok("USER","POST","/user/orders",body);id=order.path("orderId").asLong();
            JsonNode paid=ok("USER","POST","/user/orders/"+id+"/pay",null);
            ok("USER","POST","/user/orders/"+id+"/pay",null);
            assertThat(paid.path("tickets")).hasSize(1);stock(29,0);
            String code=paid.path("tickets").get(0).path("verificationCode").asText();
            JsonNode refund=ok("USER","POST","/user/orders/"+id+"/refunds",Map.of("reason","验收退款"));
            ok("PLATFORM_ADMIN","POST","/admin/platform/verifiers",Map.of("activityId",activity,"userId",base+3));
            status("VERIFIER","POST","/verify/tickets/check-in",Map.of("verificationCode",code,"idCardNo","11010519491231002X"),400);
            String review="/admin/organizer/orders/"+id+"/refunds/"+refund.path("id").asText()+"/review";
            ok("ORGANIZER_ADMIN","POST",review,Map.of("approved",false,"reason","验收驳回"));
            refund=ok("USER","POST","/user/orders/"+id+"/refunds",Map.of("reason","重新申请验收退款"));
            review="/admin/organizer/orders/"+id+"/refunds/"+refund.path("id").asText()+"/review";
            ok("ORGANIZER_ADMIN","POST",review,Map.of("approved",true,"reason","验收通过"));
            status("ORGANIZER_ADMIN","POST",review,Map.of("approved",true),400);
            assertThat(ok("USER","GET","/user/orders/"+id,null).path("status").asText()).isEqualTo("REFUNDED");stock(30,0);
            status("VERIFIER","POST","/verify/tickets/check-in",Map.of("verificationCode",code,"idCardNo","11010519491231002X"),400);
            mark("重复支付不重复出票、退款冻结/驳回/重提/通过、重复退款保护与库存回补");
            order=ok("USER","POST","/user/orders",Map.of("ticketTypeId",type,"quantity",1));
            paid=ok("USER","POST","/user/orders/"+order.path("orderId").asText()+"/pay",null);
            code=paid.path("tickets").get(0).path("verificationCode").asText();
            status("VERIFIER","POST","/verify/tickets/check-in",Map.of("verificationCode",code,"idCardNo","110105194912310021"),409);
            ok("VERIFIER","POST","/verify/tickets/check-in",Map.of("verificationCode",code,"idCardNo","11010519491231002X"));
            status("VERIFIER","POST","/verify/tickets/check-in",Map.of("verificationCode",code,"idCardNo","11010519491231002X"),409);
            assertThat(ok("VERIFIER","GET","/verify/tickets/records",null).toString()).contains(code);
            status("USER","POST","/user/orders/"+order.path("orderId").asText()+"/refunds",Map.of("reason","已核销不能退款"),400);
            ok("PLATFORM_ADMIN","GET","/admin/platform/dashboard/summary?days=7",null);
            ok("ORGANIZER_ADMIN","GET","/admin/organizer/dashboard/summary?days=7",null);
            mark("实名错误拒绝、成功核销、重复核销拒绝、已核销禁退与两端统计");
            if(Boolean.getBoolean("acceptance.browser"))browser();
            ok("PLATFORM_ADMIN","POST","/admin/platform/activities/"+activity+"/offline",Map.of("reason","验收结束"));
            status(null,"GET","/user/activities/"+activity,null,404);
            ok("USER","POST","/auth/logout",null);status("USER","GET","/user/orders",null,401);
            mark("下架隐藏活动与退出令牌即时失效");
        } finally {cleanup();}
    }
    void fixtures() {
        String url=Objects.requireNonNull(jdbc.getDataSource()).toString();
        try(var connection=jdbc.getDataSource().getConnection()) {
            url=connection.getMetaData().getURL();
        } catch(java.sql.SQLException ex){throw new IllegalStateException(ex);}
        assertThat(url).as("Acceptance permits only local development MySQL").matches("jdbc:mysql://(localhost|127\\.0\\.0\\.1):.*");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM city_user WHERE id BETWEEN ? AND ?",Long.class,base,base+5)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM organizer WHERE id BETWEEN ? AND ?",Long.class,base,base+5)).isZero();
        ownsFixtures=true;
        String hash=encoder.encode(password),identity=encoder.encode("11010519491231002X");
        List<String> roles=List.of("PLATFORM_ADMIN","ORGANIZER_ADMIN","USER","VERIFIER","OTHER_USER","OTHER_ORG");
        for(int i=0;i<roles.size();i++) {
            jdbc.update("INSERT INTO city_user(id,username,password_hash,staff_name,real_name,id_card_hash,id_card_masked) VALUES(?,?,?,?,?,?,?)",
                    base+i,prefix+roles.get(i),hash,"验收人员","验收观众",identity,"110105********002X");
            jdbc.update("INSERT INTO sys_user_role(user_id,role_id) SELECT ?,id FROM sys_role WHERE code=?",base+i,
                    roles.get(i).equals("OTHER_USER")?"USER":roles.get(i).equals("OTHER_ORG")?"ORGANIZER_ADMIN":roles.get(i));
        }
        for(int i:List.of(1,5)) {
            jdbc.update("INSERT INTO organizer(id,name,contact_name,contact_phone,status) VALUES(?,?,?,?,'APPROVED')",base+i,prefix+"机构"+i,"验收联系人","00000000000");
            jdbc.update("INSERT INTO organizer_member(organizer_id,user_id,member_role,enabled) VALUES(?,?,'OWNER',1)",base+i,base+i);
        }
        jdbc.update("INSERT INTO organizer_member(organizer_id,user_id,member_role,enabled) VALUES(?,?,'VERIFIER',1)",base+1,base+3);
    }
    void stock(int available,int locked) {
        assertThat(jdbc.queryForObject("SELECT stock FROM ticket_type WHERE id=?",Integer.class,type)).isEqualTo(available);
        assertThat(jdbc.queryForObject("SELECT locked_stock FROM ticket_type WHERE id=?",Integer.class,type)).isEqualTo(locked);
    }
    JsonNode ok(String role,String method,String path,Object body,String... header)throws Exception {
        var response=request(role,method,path,body,header);
        assertThat(response.statusCode()).as(method+" "+path+" "+response.body()).isEqualTo(200);
        JsonNode result=json.readTree(response.body());assertThat(result.path("code").asInt()).as(path).isEqualTo(1);return result.path("data");
    }
    void status(String role,String method,String path,Object body,int expected)throws Exception {
        var r=request(role,method,path,body);assertThat(r.statusCode()).as(method+" "+path+" "+r.body()).isEqualTo(expected);
        negativeChecks++;
    }
    HttpResponse<String> request(String role,String method,String path,Object body,String... header)throws Exception {
        requests++;
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:18082/controller"+path)).timeout(Duration.ofSeconds(20)).header("Content-Type","application/json");
        if(role!=null)b.header("Authorization","Bearer "+tokens.get(role));
        for(int i=0;i<header.length;i+=2)b.header(header[i],header[i+1]);
        return http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    void mark(String text){System.out.println("ACCEPTANCE PASS: "+text);}
    void browser()throws Exception {
        Path script=Path.of("../../city-veri-tick-web/scripts/four-role-acceptance.cjs").toAbsolutePath().normalize();
        var builder=new ProcessBuilder("node",script.toString()).redirectErrorStream(true);
        builder.environment().put("ACCEPTANCE_PREFIX",prefix);builder.environment().put("ACCEPTANCE_PASSWORD",password);
        builder.environment().put("ACCEPTANCE_ACTIVITY",String.valueOf(activity));
        Path browserLog=Path.of("../../docs/acceptance-browser.log").toAbsolutePath().normalize();
        builder.redirectOutput(browserLog.toFile());
        Process p=builder.start();boolean done=p.waitFor(150,TimeUnit.SECONDS);
        if(!done)p.destroyForcibly();assertThat(done).as("Browser timeout").isTrue();assertThat(p.exitValue()).isZero();mark("真实浏览器四角色登录、页面及购票核销交互");
    }
    void cleanup() {
        if(!ownsFixtures)return;
        jdbc.update("DELETE r FROM management_message_read r JOIN management_message m ON m.id=r.message_id WHERE m.content LIKE ?","%"+prefix+"%");
        jdbc.update("DELETE FROM management_message_read WHERE user_id BETWEEN ? AND ?",base,base+5);
        jdbc.update("DELETE FROM management_message WHERE content LIKE ? OR organizer_id IN (?,?)","%"+prefix+"%",base+1,base+5);
        jdbc.update("DELETE c FROM check_in_record c JOIN ticket t ON t.id=c.ticket_id JOIN ticket_order o ON o.id=t.order_id WHERE o.user_id BETWEEN ? AND ?",base,base+5);
        jdbc.update("DELETE FROM user_coupon WHERE user_id BETWEEN ? AND ?",base,base+5);
        jdbc.update("DELETE FROM refund_request WHERE user_id BETWEEN ? AND ?",base,base+5);
        jdbc.update("DELETE t FROM ticket t JOIN ticket_order o ON o.id=t.order_id WHERE o.user_id BETWEEN ? AND ?",base,base+5);
        jdbc.update("DELETE FROM ticket_order WHERE user_id BETWEEN ? AND ?",base,base+5);
        jdbc.update("DELETE FROM coupon WHERE id=?",coupon);
        jdbc.update("DELETE FROM activity WHERE organizer_id IN (?,?)",base+1,base+5);
        jdbc.update("DELETE FROM organizer WHERE id IN (?,?)",base+1,base+5);
        jdbc.update("DELETE FROM city_user WHERE id BETWEEN ? AND ?",base,base+5);
        try(var keys=redis.scan(ScanOptions.scanOptions().match(REDIS_PREFIX+"*").count(100).build())) {
            List<String> owned=new ArrayList<>();keys.forEachRemaining(owned::add);if(!owned.isEmpty())redis.delete(owned);
        }
        for(long i=base;i<=base+5;i++) {
            try(var keys=redis.scan(ScanOptions.scanOptions().match("order:create:{"+i+"}:*").count(100).build())) {
                List<String> owned=new ArrayList<>();keys.forEachRemaining(owned::add);if(!owned.isEmpty())redis.delete(owned);
            }
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM city_user WHERE username LIKE ?",Long.class,prefix+"%")).isZero();
        mark("本轮隔离账号、机构、消息、券、订单、票券和核销记录已清理");
        System.out.println("ACCEPTANCE HTTP REQUESTS: "+requests+", NEGATIVE CHECKS: "+negativeChecks);
    }
}
