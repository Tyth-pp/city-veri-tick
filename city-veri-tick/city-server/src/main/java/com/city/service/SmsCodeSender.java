package com.city.service;


public interface SmsCodeSender {
    void send(String phone, String code, int ttlSeconds);
}
