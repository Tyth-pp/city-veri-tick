package com.city.dto;

import com.city.constant.AccountMessageConstant;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StaffProfileDTO {
    @NotBlank
    @Size(max = 32)
    private String name;
    @NotBlank
    @Pattern(regexp = "1[3-9][0-9]{9}", message = AccountMessageConstant.PHONE_INVALID)
    private String phone;
}
