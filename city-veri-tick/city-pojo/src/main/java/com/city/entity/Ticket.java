package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.city.enums.TicketStatus;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("ticket")
@Data
public class Ticket implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    private String ticketNo;
    private Long orderId;
    private Long ticketTypeId;
    private String verificationCode;
    private TicketStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
