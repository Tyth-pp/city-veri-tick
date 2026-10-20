package com.city.dto;

import com.city.constant.AccountMessageConstant;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StaffAccountDTO {
    @NotBlank
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{3,31}", message = AccountMessageConstant.USERNAME_FORMAT_INVALID)
    private String username;
    @NotBlank
    @Size(max = 32)
    private String name;
    @NotBlank
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = AccountMessageConstant.PHONE_INVALID)
    private String phone;
    @NotBlank
    @Size(min = 10, max = 64)
    @Pattern(regexp = "(?s)(?=.*[A-Za-z])(?=.*[0-9]).*", message = AccountMessageConstant.INITIAL_PASSWORD_FORMAT_INVALID)
    private String initialPassword;

    @AssertTrue(message = AccountMessageConstant.INITIAL_PASSWORD_BYTES_EXCEEDED)
    public boolean isPasswordByteLengthValid() {
        return initialPassword == null || initialPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
    }
}

