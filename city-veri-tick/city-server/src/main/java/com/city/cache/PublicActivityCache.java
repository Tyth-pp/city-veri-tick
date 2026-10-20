package com.city.cache;

import com.city.constant.ActivityMessageConstant;
import com.city.result.SystemResultCode;
import com.city.entity.Activity;
import com.city.enums.ActivityStatus;
import com.city.exception.BusinessException;
import com.city.mapper.ActivityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;


/**
 * 公共活动缓存组件，用于管理活动数据的缓存和数据库访问
 * 使用Redis作为缓存存储，并实现了缓存失效、降级和限流等机制
 */
@Component
@Slf4j
public class PublicActivityCache {
    // Redis Lua脚本，用于处理缓存操作
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();

    // 初始化Redis脚本
    static {
        // 设置Lua脚本的位置
        SCRIPT.setLocation(new ClassPathResource("redis/public-activity-cache.lua"));
        // 设置脚本返回类型为Long
        SCRIPT.setResultType(Long.class);
    }

    // Redis模板，用于Redis操作
    private final StringRedisTemplate redis;
    // JSON对象映射器，用于序列化和反序列化
    private final ObjectMapper json;
    // 活动数据访问映射器
    private final ActivityMapper mapper;
    // 数据库访问信号量，限制并发数据库访问数量
    private final Semaphore databaseSlots = new Semaphore(8);
    // 降级锁数组，用于防止缓存重建时的并发问题
    private final ReentrantLock[] fallbackLocks = new ReentrantLock[64];
    // 缓存键前缀，可通过配置覆盖
    @Value("${city.cache.activity-prefix:cache:public-activity:v1:}")
    private String prefix = "cache:public-activity:v1:";
    // 绕过缓存的截止时间（纳秒），用于缓存不可用时的降级处理
    private volatile long bypassUntilNanos;

    /**
     * 构造函数，注入所需依赖
     *
     * @param redis  Redis模板
     * @param json   JSON对象映射器
     * @param mapper 活动数据访问映射器
     */
    public PublicActivityCache(StringRedisTemplate redis, ObjectMapper json, ActivityMapper mapper) {
        this.redis = redis;
        this.json = json;
        this.mapper = mapper;
        // 初始化降级锁数组
        for (int i = 0; i < fallbackLocks.length; i++) fallbackLocks[i] = new ReentrantLock();
    }

    /**
     * 获取已发布的活动信息
     *
     * @param id 活动ID
     * @return 活动信息，如果不存在或未发布则返回null
     */
    public Activity getPublished(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException(ActivityMessageConstant.ID_INVALID);
        Activity activity;
        // 如果当前时间在绕过缓存截止时间之前，则直接降级到数据库查询
        if (System.nanoTime() < bypassUntilNanos) activity = fallback(id);
        else try {
            // 尝试读取或加载缓存
            activity = readOrLoad(id);
        } catch (CacheUnavailable ex) {
            // 缓存不可用时打开断路器
            circuitOpen();
            // 降级到数据库查询
            activity = fallback(id);
        }
        if (activity == null) return null;
        return database(() -> mapper.isPublished(id)) ? activity : null;
    }

    /**
     * 读取或加载活动信息的方法，实现了缓存读取、锁机制和加载逻辑
     *
     * @param id 活动的唯一标识符
     * @return 返回活动对象
     */
    private Activity readOrLoad(Long id) {
        // 首先尝试从缓存中读取原始数据
        String raw = read(id);
        // 如果缓存中存在数据，则解码并返回
        if (raw != null) return decode(id, raw);
        // 生成锁的拥有者标识
        String owner = UUID.randomUUID().toString();
        // 尝试获取锁，设置锁的过期时间为5秒
        boolean locked = redisCall(() -> Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey(id), owner, Duration.ofSeconds(5))));
        // 如果获取锁失败
        if (!locked) {
            // 设置等待超时时间（150毫秒）
            long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(150);
            // 循环等待，直到超时或获取到数据
            do {
                pause(); // 暂一段时间
                raw = read(id);
                if (raw != null) return decode(id, raw);
            } while (System.nanoTime() < until);
            // 如果超时仍未获取到数据，抛出繁忙异常
            throw busy();
        }
        try {
            raw = read(id); // 尝试从Redis读取原始数据
            if (raw != null) return decode(id, raw); // 如果缓存存在，则解码并返回
            // 缓存不存在，从数据库加载活动
            Activity activity = load(id);
            String encoded = activity == null ? "null" : encode(activity); // 序列化活动数据
            if (encoded != null) {
                // 设置TTL：如果活动为null，则设置20-40秒；否则设置300-360秒
                long ttl = activity == null ? ThreadLocalRandom.current().nextLong(20, 41) : ThreadLocalRandom.current().nextLong(300, 361);
                try {
                    // 使用Lua脚本发布数据到缓存
                    script(id, "PUBLISH", owner, encoded, Long.toString(ttl * 1000));
                } catch (CacheUnavailable ex) {
                    circuitOpen(); // 缓存不可用时打开断路器
                }
            }
            return activity; // 返回活动对象（可能为null）
        } finally {
            // 确保锁被释放
            try {
                script(id, "RELEASE", owner);
            } catch (CacheUnavailable ex) {
                circuitOpen(); // 处理null值情况
            }
        }
    }

/**
 * 解码并验证活动信息
 * @param id 活动ID
 * @param raw 原始JSON字符串
 * @return 解码后的Activity对象，如果验证失败则返回null
 */
    private Activity decode(Long id, String raw) {
    // 如果原始字符串为"null"，直接返回null
        if ("null".equals(raw)) return null;
        try {
        // 将JSON字符串解析为Activity对象
            Activity value = json.readValue(raw, Activity.class);
        // 验证活动对象不为空，ID匹配且状态为已发布
            if (value != null && id.equals(value.getId()) && value.getStatus() == ActivityStatus.PUBLISHED)

                return value;
        } catch (JsonProcessingException ignored) {

        // 捕获JsonProcessingException异常但不做处理
        }
    // 如果验证失败，使缓存失效并返回备用结果
        invalidate(id);
        return fallback(id);
    }

    /**
     * 降级方法，当缓存不可用时的备选方案
     *
     * @param id 活动ID
     * @return 活动对象，如果不存在则返回null
     */
    private String encode(Activity value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Activity cache serialization failed: {}", ex.getClass().getSimpleName());
            return null;
        }
    }

    private Activity load(Long id) {
        return database(() -> {
            Activity value = mapper.selectById(id);
            return value != null && value.getStatus() == ActivityStatus.PUBLISHED ? value : null;
        });
    }

    /**
     * 执行数据库操作，并限制并发访问数量
     *
     * @param  operation 要执行的数据库操作
     * @return 操作结果
     */

    private Activity fallback(Long id) {
        ReentrantLock lock = fallbackLocks[Math.floorMod(id.hashCode(), fallbackLocks.length)];
        boolean acquired = false;
        try {
            acquired = lock.tryLock(150, TimeUnit.MILLISECONDS);
            if (!acquired) throw busy();
            return load(id);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw busy();
        } finally {
            if (acquired) lock.unlock();
        }
    }

    private <T> T database(Supplier<T> operation) {
        if (!databaseSlots.tryAcquire()) throw busy();
        try {
            return operation.get();
        } finally {
            databaseSlots.release();
        }
    }

    private String read(Long id) {
        return redisCall(() -> redis.opsForValue().get(dataKey(id)));
    }

    private Long script(Long id, String... args) {
        return redisCall(() -> {
            Long result = redis.execute(SCRIPT, List.of(dataKey(id), lockKey(id)), (Object[]) args);
            if (result == null) throw new CacheUnavailable();
            return result;
        });
    }

    private <T> T redisCall(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessException ex) {
            throw new CacheUnavailable();
        }
    }

    public void invalidateAfterCommit(Long id) {
        if (id == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidate(id);
                }
            });
        } else invalidate(id);
    }

    private void invalidate(Long id) {
        try {
            script(id, "INVALIDATE");
        } catch (CacheUnavailable ex) {
            circuitOpen();
            log.warn("Activity cache invalidation unconfirmed, activityId={}; bounded TTL remains", id);
        }
    }

    private void circuitOpen() {
        long now = System.nanoTime();
        if (now >= bypassUntilNanos)
            log.warn("Activity cache unavailable; bounded database fallback enabled for 2 seconds");
        bypassUntilNanos = now + TimeUnit.SECONDS.toNanos(2);
    }

    private void pause() {
        try {
            Thread.sleep(15);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw busy();
        }
    }

    private BusinessException busy() {
        return new BusinessException(SystemResultCode.ACTIVITY_QUERY_BUSY);
    }

    private String dataKey(Long id) {
        return prefix + "{" + id + "}:data";
    }

    private String lockKey(Long id) {
        return prefix + "{" + id + "}:lock";
    }

    private static class CacheUnavailable extends RuntimeException {
    }
}
