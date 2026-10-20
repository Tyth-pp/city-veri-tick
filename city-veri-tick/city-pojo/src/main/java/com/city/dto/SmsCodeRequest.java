package com.city.dto;

import com.city.constant.AccountMessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Schema(description = "短信验证码请求")
@Data
public class SmsCodeRequest implements Serializable {
    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = AccountMessageConstant.PHONE_REQUIRED_OR_INVALID)
    private String phone;
}
