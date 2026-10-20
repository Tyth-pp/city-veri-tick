package com.city.controller.auth;

import com.city.result.Result;
import com.city.dto.LoginDTO;
import com.city.dto.SmsCodeRequest;
import com.city.dto.SmsLoginDTO;
import com.city.vo.LoginVO;
import com.city.vo.SmsCodeVO;
import com.city.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/controller/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    /**
     * @param loginDTO 登录请求参数
     * @return 登录结果及访问令牌
     * @apiNote 用户登录
     */
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    @Operation(summary = "发送手机号验证码")
    @PostMapping("/sms/send")
    public Result<SmsCodeVO> sendSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        return Result.success(authService.sendSmsCode(request));
    }

    @Operation(summary = "手机号验证码登录或注册")
    @PostMapping("/sms/login")
    public Result<LoginVO> loginBySms(@Valid @RequestBody SmsLoginDTO request) {
        return Result.success(authService.loginBySms(request));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    /**
     * @param authorization 当前登录用户的认证请求头
     * @return 操作结果
     * @apiNote 用户退出登录
     */
    public Result logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring(7));
        }
        return Result.success();
    }
}
