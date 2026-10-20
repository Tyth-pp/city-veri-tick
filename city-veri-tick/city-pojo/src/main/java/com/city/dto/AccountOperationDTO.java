package com.city.dto;

import com.city.constant.AccountMessageConstant;
import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class AccountOperationDTO {
    @Size(max = 500, message = AccountMessageConstant.OPERATION_REASON_TOO_LONG)
    private String reason;
}
