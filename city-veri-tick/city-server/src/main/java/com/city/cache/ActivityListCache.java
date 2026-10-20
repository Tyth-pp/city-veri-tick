package com.city.cache;

import com.city.entity.Activity;
import com.city.mapper.ActivityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 这是一个用于存储公共活动候选者的有界缓存。动态会话、销售窗口和库存保留在MySQL中。
 */
@Component
@Slf4j
public class ActivityListCache {
    private final StringRedisTemplate redis;  // Redis模板，用于操作Redis
    private final ObjectMapper json;          // JSON对象映射器，用于序列化和反序列化
    private final ActivityMapper mapper;      // 活动映射器，用于数据库操作
    @Value("${city.cache.activity-list-prefix:cache:public-activity-list:v1:}")
    private String prefix = "cache:public-activity-list:v1:";  // Redis键前缀
    private static final int MAX_INPUT_LENGTH = 256;  // 最大输入长度限制

    /**
     * 构造函数，注入Redis模板、JSON对象映射器和活动映射器
     * @param redis Redis模板
     * @param json JSON对象映射器
     * @param mapper 活动映射器
     */
    public ActivityListCache(StringRedisTemplate redis, ObjectMapper json, ActivityMapper mapper) {
        this.redis = redis;
        this.json = json;
        this.mapper = mapper;
    }

    /**
     * 获取已发布的活动列表
     * @param keyword 搜索关键词
     * @param category 活动分类
     * @param loader 数据加载器，用于从数据库加载数据
     * @return 已发布的活动列表
     */
    public List<Activity> getPublished(String keyword, String category, Supplier<List<Activity>> loader) {
        String normalized = keyword == null ? null : keyword.trim();  // 规范化关键词
        String cat = category == null ? null : category.trim();        // 规范化分类
        normalized = normalize(normalized);  // 进一步规范化
        cat = normalize(cat);               // 进一步规范化
        try {
            String key = key(normalized, cat);  // 生成缓存键
            String raw = redis.opsForValue().get(key);  // 从Redis获取原始数据
            if (raw != null) {
                List<Activity> cached = decode(raw);  // 解码缓存数据
                if (cached != null)
                    return cached.stream().filter(a -> a.getStatus() != null && a.getStatus().name().equals("PUBLISHED") && mapper.isPublished(a.getId())).toList();  // 过滤已发布的活动
            }
            List<Activity> loaded = loader.get();  // 从数据库加载数据
            String encoded = encode(loaded);      // 编码数据
            if (encoded != null)
                redis.opsForValue().set(key, encoded, Duration.ofSeconds(ThreadLocalRandom.current().nextLong(30, 61)));  // 设置缓存，随机过期时间30-60秒
            return loaded;  // 返回加载的数据
        } catch (DataAccessException ex) {
            log.warn("Activity list cache unavailable; using database query: {}", ex.getClass().getSimpleName());  // 缓存不可用，记录警告并使用数据库查询
            return loader.get();  // 从数据库加载数据
        }
    }

    /**
     * 在事务提交后使缓存失效
     */
    public void invalidateAfterCommit() {
        Runnable op = () -> {
            try {
                redis.opsForValue().increment(versionKey());  // 增加版本号，使缓存失效
            } catch (DataAccessException ex) {
                log.warn("Activity list cache version invalidation unconfirmed: {}", ex.getClass().getSimpleName());  // 版本失效确认失败，记录警告
            }
        };
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())  // 检查事务同步是否激活
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {  // 注册事务同步
                @Override
                public void afterCommit() {
                    op.run();  // 事务提交后执行操作
                }
            });
        else op.run();  // 如果事务同步未激活，直接执行操作
    }

    /**
     * 解码JSON字符串为活动列表
     * @param raw JSON字符串
     * @return 活动列表，解码失败返回null
     */
    private List<Activity> decode(String raw) {
        try {
            JavaType t = json.getTypeFactory().constructCollectionType(List.class, Activity.class);  // 构造列表类型
            return json.readValue(raw, t);  // 读取JSON数据到活动列表
        } catch (Exception ex) {
            log.warn("Activity list cache value discarded: {}", ex.getClass().getSimpleName());  // 记录警告并丢弃缓存值
            return null;
        }
    }

    /**
     * 编码活动列表为JSON字符串
     * @param value 活动列表
     * @return JSON字符串，编码失败返回null
     */
    private String encode(List<Activity> value) {
        try {
            return json.writeValueAsString(value);  // 将活动列表写入JSON字符串
        } catch (JsonProcessingException ex) {
            return null;  // 编码失败返回null
        }
    }

    /**
     * 生成缓存键
     * @param keyword 关键词
     * @param category 分类
     * @return 缓存键字符串
     */
    private String key(String keyword, String category) {
        return prefix + version() + ":" + digest(keyword) + ":" + digest(category);  // 组合前缀、版本和摘要生成键
    }

    /**
     * 获取当前版本号
     * @return 版本号字符串
     */
    private String version() {
        String v = redis.opsForValue().get(versionKey());  // 从Redis获取版本号
        if (v == null) {
            redis.opsForValue().setIfAbsent(versionKey(), "1");  // 如果版本号不存在，设置为1
            v = redis.opsForValue().get(versionKey());  // 再次获取版本号
        }
        return v == null ? "1" : v;  // 返回版本号，如果为null则返回1
    }

    /**
     * 获取版本键
     * @return 版本键字符串
     */
    private String versionKey() {
        return prefix + "version";  // 组合前缀和"version"生成版本键
    }

    /**
     * 规范化字符串
     * @param value 输入字符串
     * @return 规范化后的字符串，如果为空或空白则返回null
     */
    private String normalize(String value) {
        if (value == null) return null;  // 如果输入为null，返回null
        String normalized = value.trim();  // 去除前后空格
        return normalized.isEmpty() ? null : normalized;
    }  // 如果为空字符串则返回null，否则返回规范化后的字符串

    private String digest(String value) {
    /**
     * 生成字符串的SHA-256摘要
     * @param value 输入字符串
     * @return 摘要字符串，如果输入为null则返回"-"
     */
        if (value == null) return "-";
        try {  // 如果输入为null，返回"-"
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);  // 计算SHA-256摘要
            for (byte b : bytes) out.append(String.format("%02x", b));  // 创建字符串构建器
            return out.toString();  // 将每个字节转换为十六进制字符串
        } catch (Exception ex) {  // 返回摘要字符串
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }  // 如果SHA-256不可用，抛出异常
    }
}
