package com.city.dto;

import com.city.constant.RefundMessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundApplyDTO {
    @NotBlank(message = RefundMessageConstant.REASON_REQUIRED)
    @Size(max = 500, message = RefundMessageConstant.REASON_TOO_LONG)
    private String reason;
}
