package com.city.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Jackson配置类，用于配置Jackson序列化/反序列化规则
 * 本配置类主要解决Long类型数据序列化为字符串的问题，避免前端处理大数字时精度丢失
 */
@Configuration
public class JacksonConfig {
    /**
     * 创建并配置一个SimpleModule模块，将Long类型数据序列化为字符串
     *
     * @return 配置好的SimpleModule实例
     */
    @Bean
    public Module longAsStringModule() {
        // 创建名为"long-as-string"的模块
        SimpleModule module = new SimpleModule("long-as-string");
        // 创建Long类型的序列化器，将Long值转换为字符串形式
        JsonSerializer<Long> serializer = new JsonSerializer<>() {
            /**
             * 重写serialize方法，实现Long到字符串的转换
             *
             * @param value 要序列化的Long值
             * @param gen JSON生成器，用于生成JSON输出
             * @param serializers 序列化提供器，可用于获取其他序列化器
             * @throws IOException 如果发生I/O错误
             */
            @Override
            public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                // 如果value为null，则输出null，否则将Long值转换为字符串输出
                gen.writeString(value == null ? null : value.toString());
            }
        };
        // 将序列化器添加到模块中，分别处理Long.class和long基本类型
        module.addSerializer(Long.class, serializer);
        module.addSerializer(Long.TYPE, serializer);
        // 返回配置好的模块
        return module;
    }
}
