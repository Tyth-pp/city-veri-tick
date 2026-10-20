package com.city.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 上海时区LocalDateTime序列化器
 * 用于将LocalDateTime对象按照上海时区转换为ISO_OFFSET_DATE_TIME格式的字符串
 */
public class ShanghaiOffsetDateTimeSerializer extends JsonSerializer<LocalDateTime> {
    // 定义上海时区常量
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 重写serialize方法，实现LocalDateTime的序列化
     * @param value 要序列化的LocalDateTime对象
     * @param generator Json生成器，用于生成JSON输出
     * @param provider 序列化提供器，可用于获取默认配置
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider provider)
            throws IOException {
        // 将LocalDateTime转换为上海时区的ZonedDateTime，然后格式化为ISO_OFFSET_DATE_TIME字符串
        generator.writeString(value.atZone(ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
}
