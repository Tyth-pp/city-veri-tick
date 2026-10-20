package com.city.cache;

import com.city.result.SystemResultCode;
import com.city.entity.Coupon;
import com.city.exception.BusinessException;
import com.city.mapper.CouponMapper;
import com.city.mapper.UserCouponMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Redis的准入是临时的，MySQL仍然是已发放优惠券的权威数据源。
 */
@Component
@Slf4j
public class CouponClaimGate {
    // 使用Redis脚本处理优惠券领取逻辑
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();

    static {
        // 设置Lua脚本的位置和返回类型
        SCRIPT.setLocation(new ClassPathResource("redis/coupon-claim.lua"));
        SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redis; // Redis字符串模板，用于Redis操作
    private final CouponMapper coupons; // 优惠券数据访问层
    private final UserCouponMapper users; // 用户优惠券数据访问层
    private final TransactionTemplate bootstrap; // 事务模板
    private final Clock clock; // 时钟对象，用于获取时间信息
    @Value("${city.coupon.claim-prefix:coupon:claim:v1:}")
    private String prefix = "coupon:claim:v1:"; // Redis键前缀
    private static final long SNAPSHOT_MILLIS = 60_000;

    public record Reservation(Long couponId, Long userId, String requestId) {
    }

/**
 * 优惠券领取门面类的构造函数
 * @param redis Redis字符串模板，用于Redis操作
 * @param coupons 优惠券数据访问层，用于优惠券相关数据库操作
 * @param users 用户优惠券数据访问层，用于用户优惠券相关数据库操作
 * @param manager 平台事务管理器，用于管理事务
 * @param clock 时钟对象，用于获取时间信息
 */
    public CouponClaimGate(StringRedisTemplate redis, CouponMapper coupons, UserCouponMapper users,
                           PlatformTransactionManager manager, Clock clock) {
    // 将传入的Redis模板赋值给类成员变量
        this.redis = redis;
    // 将传入的优惠券数据访问层赋值给类成员变量
        this.coupons = coupons;
    // 将传入的用户优惠券数据访问层赋值给类成员变量
        this.users = users;
    // 将传入的时钟对象赋值给类成员变量
        this.clock = clock;
    // 创建事务模板实例，并设置事务传播行为为REQUIRES_NEW
        bootstrap = new TransactionTemplate(manager);
        bootstrap.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 生成优惠券的Redis键
     * @param couponId 优惠券ID
     * @return 返回拼接后的Redis键
     */
    private String key(Long couponId) {
        return prefix + "{" + couponId + "}";
    }

    /**
     * 预留优惠券的方法
     * @param couponId 优惠券ID
     * @param userId 用户ID
     * @return 返回预留对象，预留成功则返回，失败则返回null
     * @throws BusinessException 当优惠券不可用或系统繁忙时抛出
     */
    public Reservation reserve(Long couponId, Long userId) {
        // 创建预留对象，包含优惠券ID、用户ID和唯一请求ID
        Reservation ticket = new Reservation(couponId, userId, UUID.randomUUID().toString());
        try {
            // 尝试执行预留操作
            Long result = run(couponId, "reserve", userId.toString(), ticket.requestId(), "-1");
            // 如果结果为-1或-6，表示需要特殊处理
            if (result != null && (result == -1 || result == -6)) {
                return bootstrap.execute(status -> {
                    Coupon c = coupons.selectForUpdate(couponId);
                    if (c == null) throw new BusinessException(SystemResultCode.COUPON_UNAVAILABLE);
                    if (!special(c)) return null;
                    initialize(c);
                    check(run(couponId, "reserve", userId.toString(), ticket.requestId(), String.valueOf(users.countClaims(userId, couponId))));
                    return ticket;
                });
            }
            check(result);
            return ticket;
        } catch (DataAccessException ex) {
            finish(ticket, false);
            Boolean ordinary = bootstrap.execute(status -> {
                Coupon c = coupons.selectForUpdate(couponId);
                return c != null && !special(c);        });
            if (Boolean.TRUE.
    equals(ordinary)) return null;
            log.warn("Coupon admission unavailable: {}", ex.getClass().getSimpleName());
            throw new BusinessException(SystemResultCode.COUPON_CLAIM_BUSY);
        }
    }

    private void initialize(Coupon c) {
        long start = c.getStartsAt().atZone(clock.getZone()).toInstant().toEpochMilli();
        long end = c.getEndsAt().atZone(clock.getZone()).toInstant().toEpochMilli();
        Long result = run(c.getId(), "init", UUID.randomUUID().toString(), String.valueOf(Math.max(0, c.getTotalQuantity() - c.getClaimedQuantity())),
                String.valueOf(c.getPerUserLimit()), String.valueOf(start), String.valueOf(end), "ACTIVE".equals(c.getStatus()) ? "1" : "0", String.valueOf(SNAPSHOT_MILLIS));
        if (result == null || result != 1) throw new BusinessException(SystemResultCode.COUPON_CLAIM_BUSY);
    }

    public static boolean special(Coupon c) {
        return "LIMITED".equals(c.getClaimMode()) || "FLASH_SALE".equals(c.getClaimMode());
    }

    private Long run(Long id, String... args) {
        return redis.execute(SCRIPT, List.of(key(id)), (Object[]) args);
    }

    private void check(Long code) {
        if (code != null && code == 1) return;
        SystemResultCode result = code == null ? SystemResultCode.COUPON_CLAIM_BUSY : switch (code.intValue()) {
            case -2 -> SystemResultCode.COUPON_CLAIM_NOT_STARTED;
            case -3 -> SystemResultCode.COUPON_CLAIM_ENDED;
            case -4 -> SystemResultCode.COUPON_CLAIM_LIMIT;
            case -5 -> SystemResultCode.COUPON_UNAVAILABLE;
            case -7 -> SystemResultCode.COUPON_SOLD_OUT;
            default -> SystemResultCode.COUPON_CLAIM_BUSY;
        };
        throw new BusinessException(result);
    }

/**
 * 完成优惠券的最终处理
 * @param ticket 优惠券预订信息对象，包含优惠券ID、请求ID和用户ID等信息
 * @param committed 布尔值，表示是否提交操作。true表示提交，false表示回滚
 */
    public void finish(Reservation ticket, boolean committed) {
    // 检查ticket对象是否为空，如果为空则直接返回
        if (ticket == null) return;
        try {
        // 执行最终的优惠券处理操作
        // 参数包括：优惠券ID、操作类型("finish")、请求ID、用户ID和操作类型(commit/rollback)
            run(ticket.couponId(), "finish", ticket.requestId(), ticket.userId().toString(), committed ? "commit" : "rollback");
        } catch (DataAccessException ex) {
        // 捕获数据访问异常，记录警告日志
        // 当出现异常时，系统将延迟优惠券处理的最终完成，等待快照过期
            log.warn("Coupon admission finalization deferred to snapshot expiry: {}", ex.getClass().getSimpleName());
        }
    }

    public void invalidateAfterCommit(Long couponId) {
        Runnable remove = () -> {
            try {
                redis.delete(key(couponId));
            } catch (RuntimeException ex) {
                log.warn("Coupon admission invalidation deferred to snapshot expiry: {}", ex.getClass().getSimpleName());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    remove.run();
                }
            });
        else remove.run();
    }
}
