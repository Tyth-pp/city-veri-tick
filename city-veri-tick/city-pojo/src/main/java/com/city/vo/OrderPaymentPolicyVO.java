package com.city.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Data
@AllArgsConstructor
public class OrderPaymentPolicyVO {
    @Schema(description = "订单支付时限，单位秒", example = "900")
    private long paymentTimeoutSeconds;
    @Schema(description = "当前服务器时间，UTC时间戳，用于客户端校时", example = "2030-01-01T02:00:00Z")
    private Instant serverTime;
}
