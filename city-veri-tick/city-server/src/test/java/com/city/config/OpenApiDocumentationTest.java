package com.city.config;

import com.city.controller.user.UserCouponController;
import com.city.controller.user.UserOrderController;
import com.city.controller.admin.platform.PlatformCouponController;
import com.city.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises generated OpenAPI without starting business infrastructure or scheduled tasks. */
@SpringBootTest(classes=OpenApiDocumentationTest.DocsApplication.class,properties={
 "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc(addFilters=false)
class OpenApiDocumentationTest {
 @Configuration(proxyBeanMethods=false)
 @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class,RedisAutoConfiguration.class,RedisRepositoriesAutoConfiguration.class})
 @Import({UserCouponController.class,PlatformCouponController.class,UserOrderController.class})
 static class DocsApplication {
  @Bean CouponService coupons(){return mock(CouponService.class);}
  @Bean OrderService orders(){return mock(OrderService.class);}
  @Bean RefundService refunds(){return mock(RefundService.class);}
 }
 @Autowired MockMvc mvc;
 @Test void publishesChineseOperationsAndActualUnlimitedRules() throws Exception {
  mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
    .andExpect(jsonPath("$.paths['/controller/user/coupons/{couponId}/claim'].post.summary").value("领取优惠券"))
    .andExpect(jsonPath("$.paths['/controller/user/coupons/available'].get.summary").value("查询领券中心列表"))
    .andExpect(jsonPath("$.paths['/controller/user/coupons/mine'].get.summary").value("查询我的优惠券及状态统计"))
    .andExpect(jsonPath("$.paths['/controller/user/coupons/choices'].get.summary").value("试算当前订单可用优惠券"))
    .andExpect(jsonPath("$.paths['/controller/user/coupons'].get.summary").value("查询本人领券原始记录"))
    .andExpect(jsonPath("$.paths['/controller/admin/platform/coupons'].get.summary").value("查询优惠券投放列表"))
    .andExpect(jsonPath("$.paths['/controller/admin/platform/coupons'].post.summary").value("创建未启用优惠券"))
    .andExpect(jsonPath("$.paths['/controller/admin/platform/coupons/{id}/status'].patch.summary").value("启用或停用优惠券投放"))
    .andExpect(jsonPath("$.paths['/controller/user/orders/payment-policy'].get.summary").value("查询订单支付时限"))
    .andExpect(jsonPath("$.paths['/controller/user/orders/{orderId}/sync-expiry'].post.summary").value("同步本人订单超时状态"))
    .andExpect(jsonPath("$.components.schemas.CreateCouponDTO.properties.perUserLimit.description").value(org.hamcrest.Matchers.containsString("0表示不限次数")))
    .andExpect(jsonPath("$.components.schemas.CreateCouponDTO.properties.maxDiscountAmount.description").value(org.hamcrest.Matchers.containsString("null表示不封顶")));
 }
}
