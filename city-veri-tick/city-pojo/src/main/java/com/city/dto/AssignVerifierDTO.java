package com.city.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AssignVerifierDTO implements Serializable {
    @NotNull
    private Long activityId;
    @NotNull
    private Long userId;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
}
