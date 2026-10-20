package com.city.dto;

import com.city.constant.TicketMessageConstant;
import jakarta.validation.constraints.Digits;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateTicketTypeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotBlank(message = TicketMessageConstant.NAME_REQUIRED)
    private String name;
    @NotNull(message = TicketMessageConstant.PRICE_REQUIRED)
    @DecimalMin(value = "0.00", message = TicketMessageConstant.PRICE_NEGATIVE)
    @Digits(integer = 8, fraction = 2, message = TicketMessageConstant.PRICE_PRECISION_INVALID)
    private BigDecimal price;
    @NotNull(message = TicketMessageConstant.STOCK_REQUIRED)
    @Min(value = 0, message = TicketMessageConstant.STOCK_NEGATIVE)
    private Integer stock;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2029-12-01T00:00:00")
    private LocalDateTime saleStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2030-01-01T09:30:00")
    private LocalDateTime saleEndTime;
    @NotNull(message = TicketMessageConstant.STATUS_REQUIRED)
    private Boolean enabled;
}
