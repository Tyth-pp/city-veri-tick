package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.city.json.ShanghaiOffsetDateTimeSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_request")
public class RefundRequest {
    @TableId private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private String reason;
    private String reviewReason;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerUsername;
    private String paymentMode;
    private String refundNo;
    @JsonSerialize(using=ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime requestedAt;
    @JsonSerialize(using=ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime processedAt;
}
