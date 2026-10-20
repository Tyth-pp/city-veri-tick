package com.city.dto;

import com.city.constant.AccountMessageConstant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateOrganizerDTO {
    @NotBlank
    @Size(max = 128)
    private String name;
    @Size(max = 32)
    private String unifiedSocialCreditCode;
    @NotBlank
    @Size(max = 64)
    private String contactName;
    @NotBlank
    @Pattern(regexp = "1[3-9][0-9]{9}", message = AccountMessageConstant.CONTACT_PHONE_INVALID)
    private String contactPhone;
    @Valid
    @NotNull
    private StaffAccountDTO administrator;
}

