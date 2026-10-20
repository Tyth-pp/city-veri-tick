package com.city.dto;

import com.city.constant.AccountMessageConstant;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 修改密码的数据传输对象(DTO)
 * 用于封装修改密码请求的数据，并包含相关的验证逻辑
 */
@Data
public class ChangePasswordDTO {
    /**
     * 当前密码
     * 不能为空，最大长度为64个字符
     */
    @NotBlank
    @Size(max = 64)
    private String currentPassword;
    /**
     * 新密码
     * 不能为空，长度必须在10-64个字符之间
     * 必须包含至少一个字母和一个数字
     */
    @NotBlank
    @Size(min = 10, max = 64)
    @Pattern(regexp = "(?s)(?=.*[A-Za-z])(?=.*[0-9]).*", message = AccountMessageConstant.NEW_PASSWORD_FORMAT_INVALID)
    private String newPassword;

    /**
     * 验证新密码的UTF-8字节长度是否有效
     * 新密码的UTF-8编码长度不能超过72字节
     * @return 如果密码为null或长度不超过72字节则返回true，否则返回false
     */
    @AssertTrue(message = AccountMessageConstant.NEW_PASSWORD_BYTES_EXCEEDED)
    public boolean isPasswordByteLengthValid() {
        return newPassword == null || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
    }
}

