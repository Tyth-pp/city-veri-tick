package com.city.service;

import com.city.dto.LoginDTO;
import com.city.dto.SmsCodeRequest;
import com.city.dto.SmsLoginDTO;
import com.city.vo.LoginVO;
import com.city.vo.SmsCodeVO;

public interface AuthService {
    /**
     * @param loginDTO 登录请求参数
     * @return 登录结果及访问令牌
     * @apiNote 校验账号密码并生成访问令牌
     */
    LoginVO login(LoginDTO loginDTO);

    SmsCodeVO sendSmsCode(SmsCodeRequest request);

    LoginVO loginBySms(SmsLoginDTO request);

    /**
     * @param token 当前访问令牌
     * @return 无返回值
     * @apiNote 注销当前访问令牌
     */
    void logout(String token);

    /**
     * @param userId 目标用户ID
     * @return 无返回值
     * @apiNote 撤销目标用户的全部访问令牌
     */
    void forceLogout(Long userId);
}
