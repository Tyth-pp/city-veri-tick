package com.city.vo;
import com.city.entity.RefundRequest;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.city.json.ShanghaiOffsetDateTimeSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "订单视图")
@Data
public class OrderViewVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private java.time.Instant serverTime;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String realName;
    private String idCardMasked;
    private Long activityId;
    private String activityTitle;
    private String coverFileUrl;
    private String venueName;
    private Long organizerId;
    private String organizerName;
    private Long sessionId;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime sessionStartTime;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime sessionEndTime;
    private Long ticketTypeId;
    private String ticketTypeName;
    private Integer quantity;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private Long couponId;
    private String couponName;
    private BigDecimal payAmount;
    private String status;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime expiredAt;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime paidAt;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime createdAt;
    private List<TicketViewVO> tickets;
    private RefundRequest latestRefund;
    private List<RefundRequest> refundHistory;
    private Boolean refundEligible;
    private String refundBlockedReason;
    private String refundRule;
}
