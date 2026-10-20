package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("ticket_type")
@Data
public class TicketType implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    private Long sessionId;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private Integer lockedStock;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
