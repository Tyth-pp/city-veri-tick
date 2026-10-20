package com.city.handler;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InfrastructureFailureResponseTest {
    @RestController static class FailureController {
        @GetMapping("/test/infrastructure-failure") public void fail() {
            throw new DataAccessResourceFailureException("private-backend-details");
        }
    }
    @Test void infrastructureFailureReturnsRetryable503WithoutLeakingDetails() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailureController()).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/test/infrastructure-failure"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value(50301))
            .andExpect(jsonPath("$.msg").value("服务暂时不可用，请稍后重试；如操作结果未确认，请刷新后查看。"));
    }
}
