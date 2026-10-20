package com.city.dto;

import com.city.constant.AccountMessageConstant;
import com.city.constant.AuthMessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Schema(description = "手机号验证码登录请求")
@Data
public class SmsLoginDTO implements Serializable {
    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = AccountMessageConstant.PHONE_REQUIRED_OR_INVALID)
    private String phone;

    @NotBlank(message = AuthMessageConstant.SMS_CODE_INVALID)
    @Pattern(regexp = "^\\d{6}$", message = AuthMessageConstant.SMS_CODE_INVALID)
    private String code;
}
