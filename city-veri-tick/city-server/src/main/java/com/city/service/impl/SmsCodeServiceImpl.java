package com.city.service.impl;

import com.city.constant.AccountMessageConstant;
import com.city.service.SmsCodeService;

import com.city.result.SystemResultCode;
import com.city.exception.BusinessException;
import com.city.service.SmsCodeSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

@Service
public class SmsCodeServiceImpl implements SmsCodeService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SmsCodeServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TTL = 300, COOLDOWN = 60, ERROR_WINDOW = 600, MAX_ERRORS = 5, LEASE = 30;
    private static final DefaultRedisScript<Long> SCRIPT = script();
    @Autowired
    private StringRedisTemplate redis;
    @Autowired(required = false)
    private SmsCodeSender sender;
    @Value("${city.sms.redis-prefix:login:sms:}")
    private String prefix = "login:sms:";

    private static DefaultRedisScript<Long> script() {
        var s = new DefaultRedisScript<Long>();
        s.setLocation(new ClassPathResource("redis/sms-codes.lua"));
        s.setResultType(Long.class);
        return s;
    }

    @Override
    public int send(String phone) {
        if (sender == null) throw new BusinessException(SystemResultCode.SMS_CHANNEL_UNAVAILABLE);
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        check(execute(phone, "SEND", code, Integer.toString(TTL), Integer.toString(COOLDOWN), Integer.toString(ERROR_WINDOW), Integer.toString(MAX_ERRORS)));
        try {
            sender.send(phone, code, TTL);
        } catch (RuntimeException e) {
            try {
                execute(phone, "DELIVERY_FAILED", code);
            } catch (DataAccessException cleanup) {
                log.warn("SMS delivery cleanup unavailable: failure={}", cleanup.getClass().getSimpleName());
            }
            throw new DataAccessResourceFailureException("SMS delivery was not confirmed");
        }
        return COOLDOWN;
    }

    /**
     * Reserve one verification attempt while database account checks run.
     */
    @Override
    public String reserve(String phone, String code) {
        String lease = UUID.randomUUID().toString();
        check(execute(phone, "RESERVE", code, lease, Integer.toString(LEASE), Integer.toString(ERROR_WINDOW), Integer.toString(MAX_ERRORS)));
        return lease;
    }

    @Override
    public void complete(String phone, String lease) {
        check(execute(phone, "COMPLETE", lease));
    }

    @Override
    public void release(String phone, String lease) {
        execute(phone, "RELEASE", lease);
    }

    @Override
    public void releaseQuietly(String phone, String lease) {
        try {
            release(phone, lease);
        } catch (DataAccessException e) {
            log.warn("SMS reservation release unavailable: failure={}", e.getClass().getSimpleName());
        }
    }

    private void check(long result) {
        if (result == 1) return;
        if (result == -1) throw new BusinessException(SystemResultCode.SMS_SEND_TOO_FREQUENT);
        if (result == -2) throw new BusinessException(SystemResultCode.SMS_ATTEMPTS_EXCEEDED);
        if (result == -3) throw new BusinessException(SystemResultCode.SMS_VERIFICATION_BUSY);
        throw new BusinessException(SystemResultCode.SMS_CODE_INVALID);
    }

    private long execute(String phone, String operation, String... arguments) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) throw new IllegalArgumentException(AccountMessageConstant.PHONE_REQUIRED_OR_INVALID);
        String base = prefix + "{" + phone + "}:";
        Object[] args = new Object[arguments.length + 1];
        args[0] = operation;
        System.arraycopy(arguments, 0, args, 1, arguments.length);
        try {
            Long result = redis.execute(SCRIPT, List.of(base + "code", base + "cooldown", base + "fail", base + "lease"), args);
            if (result == null) throw new DataAccessResourceFailureException("Redis did not confirm SMS operation");
            return result;
        } catch (DataAccessException e) {
            log.warn("SMS Redis operation failed: operation={}, failure={}", operation, e.getClass().getSimpleName());
            throw e;
        }
    }
}
