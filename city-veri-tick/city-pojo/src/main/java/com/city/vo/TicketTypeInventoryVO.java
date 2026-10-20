package com.city.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class TicketTypeInventoryVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long ticketTypeId;
    private Integer stock;
    private Integer lockedStock;
    private Integer totalStock;
}
