package com.city.integration.sms;

import com.city.service.SmsCodeSender;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Development simulation only. Never registered in a non-dev deployment.
 */
@Component
@Profile("dev")
public class DevelopmentSmsCodeSender implements SmsCodeSender {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DevelopmentSmsCodeSender.class);

    @Override
    public void send(String phone, String code, int ttlSeconds) {
        log.info("[DEV SMS SIMULATION] Venuo login code: phone={}, code={}, expiresIn={}s", phone, code, ttlSeconds);
    }
}
