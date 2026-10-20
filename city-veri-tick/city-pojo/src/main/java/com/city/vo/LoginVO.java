package com.city.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Schema(description = "登录响应")
@Data
public class LoginVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String role;
    private boolean mustChangePassword;

    public LoginVO(String accessToken, String role) {
        this.accessToken = accessToken;
        this.role = role;
    }
}
