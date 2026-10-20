package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("ticket_order")
@Data
public class TicketOrder implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    private String orderNo;
    private Long userId;
    private String idempotencyKey;
    private Long couponId;
    private String realName;
    private String idCardHash;
    private String idCardMasked;
    private Long ticketTypeId;
    private Integer quantity;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String status;
    private LocalDateTime expiredAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
