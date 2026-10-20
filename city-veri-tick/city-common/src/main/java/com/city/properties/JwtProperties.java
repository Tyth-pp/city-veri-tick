package com.city.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT属性配置类
 * 用于从配置文件中读取JWT相关的配置属性
 */
@Component
@ConfigurationProperties(prefix = "city.jwt") // 从配置文件中读取前缀为"city.jwt"的属性
@Data // Lombok注解，自动生成getter、setter、toString等方法
public class JwtProperties {
    private String secret; // JWT的密钥，用于签名和验证令牌
    private Duration ttl; // JWT的过期时间，默认为12小时
    private String issuer = ""; // JWT的签发者，默认为"city-veri-tick"
    private String blacklistPrefix = ""; // JWT黑名单前缀，用于标记已失效的令牌
    private String userTokensPrefix = ""; // 用户JWT令牌前缀，用于存储用户相关的令牌信息
}
