package com.city.dto;

import com.city.constant.AccountMessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Schema(description = "验票请求")
@Data
public class VerifyTicketDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String verificationCode;
    @NotBlank(message = AccountMessageConstant.ID_CARD_REQUIRED)
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = AccountMessageConstant.ID_CARD_INVALID)
    private String idCardNo;
}
