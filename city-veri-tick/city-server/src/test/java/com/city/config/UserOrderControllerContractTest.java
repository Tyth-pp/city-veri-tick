package com.city.config;
import com.city.security.LoginPrincipal;

import com.city.controller.user.UserOrderController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.assertThat;

class UserOrderControllerContractTest {
    @Test
    void declaresStableRequestParameterNames() throws Exception {
        Method list = UserOrderController.class.getMethod("list",
                LoginPrincipal.class, String.class);
        Parameter status = list.getParameters()[1];
        assertThat(status.getAnnotation(RequestParam.class).value()).isEqualTo("status");

        Method detail = UserOrderController.class.getMethod("detail",
                LoginPrincipal.class, Long.class);
        Parameter orderId = detail.getParameters()[1];
        assertThat(orderId.getAnnotation(PathVariable.class).value()).isEqualTo("orderId");
    }
}
