package com.city.service;

/**
 * 短信验证码发送、校验预约及一次性消费的业务契约。
 */
public interface SmsCodeService {
    int send(String phone);

    String reserve(String phone, String code);

    void complete(String phone, String lease);

    void release(String phone, String lease);

    void releaseQuietly(String phone, String lease);
}
