package com.city.dto;

import com.city.constant.ActivityMessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class RejectActivityDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 500, message = ActivityMessageConstant.REJECTION_REASON_TOO_LONG)
    private String reason;
}
