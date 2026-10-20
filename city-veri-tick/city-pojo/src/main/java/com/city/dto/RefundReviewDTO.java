package com.city.dto;

import com.city.constant.RefundMessageConstant;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundReviewDTO {
    @NotNull(message = RefundMessageConstant.REVIEW_RESULT_REQUIRED)
    private Boolean approved;
    @Size(max = 500, message = RefundMessageConstant.REVIEW_EXPLANATION_TOO_LONG)
    private String reason;
}
