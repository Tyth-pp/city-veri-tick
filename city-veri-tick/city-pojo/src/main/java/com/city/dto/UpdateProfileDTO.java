package com.city.dto;

import com.city.constant.AccountMessageConstant;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileDTO {
    @Size(min = 2, max = 24)
    private String nickname;
    private Long avatarFileId;
    @Size(min = 2, max = 32)
    private String realName;
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = AccountMessageConstant.ID_CARD_INVALID)
    private String idCardNo;
}
