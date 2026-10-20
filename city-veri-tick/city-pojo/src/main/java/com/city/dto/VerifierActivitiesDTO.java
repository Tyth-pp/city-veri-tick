package com.city.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class VerifierActivitiesDTO {
    @NotNull
    @Size(max = 200)
    private List<@NotNull @Positive Long> activityIds;
}

