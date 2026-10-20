package com.city.vo;

import com.city.entity.TicketType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.city.json.ShanghaiOffsetDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PlatformTicketTypeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private Integer lockedStock;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime saleStartTime;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime saleEndTime;
    private Boolean enabled;

    public static PlatformTicketTypeVO from(TicketType type) {
        return new PlatformTicketTypeVO(type.getId(), type.getSessionId(), type.getName(), type.getPrice(),
                type.getStock(), type.getLockedStock(), type.getSaleStartTime(), type.getSaleEndTime(), type.getEnabled());
    }
}
