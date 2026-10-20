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
public class UserTicketTypeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer availableStock;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime saleStartTime;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime saleEndTime;
    private Boolean enabled;

    public static UserTicketTypeVO from(TicketType type) {
        int lockedStock = type.getLockedStock() == null ? 0 : type.getLockedStock();
        return new UserTicketTypeVO(type.getId(), type.getName(), type.getPrice(),
                Math.max(0, type.getStock()), type.getSaleStartTime(), type.getSaleEndTime(),
                type.getEnabled());
    }
}
