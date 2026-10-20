package com.city.dto;

import com.city.constant.ActivityMessageConstant;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UpdateActivitySessionDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotNull(message = ActivityMessageConstant.SESSION_START_REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2030-01-01T10:00:00")
    private LocalDateTime startTime;
    @NotNull(message = ActivityMessageConstant.SESSION_END_REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2030-01-01T12:00:00")
    private LocalDateTime endTime;
    @NotNull(message = ActivityMessageConstant.SESSION_CAPACITY_REQUIRED)
    @Min(value = 1, message = ActivityMessageConstant.SESSION_CAPACITY_INVALID)
    private Integer capacity;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime saleStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime saleEndTime;
}
