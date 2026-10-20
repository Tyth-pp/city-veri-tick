package com.city.security;

import com.city.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 认证失败响应处理器组件
 * 用于处理认证失败时的响应信息，将错误信息以JSON格式返回给客户端
 */
@Component
public class AuthenticationFailureResponse {
    private final ObjectMapper mapper;

    /**
     * 构造函数，通过依赖注入方式获取ObjectMapper实例
     * @param mapper JSON序列化工具，用于将对象转换为JSON格式
     */
    public AuthenticationFailureResponse(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 写入认证失败响应
     * @param response HTTP响应对象
     * @param status HTTP状态码
     * @param code 业务错误码
     * @param message 错误信息
     * @throws IOException 当写入响应发生IO异常时抛出
     */
    public void write(HttpServletResponse response, int status, int code, String message) throws IOException {
        response.setStatus(status); // 设置HTTP状态码
        response.setContentType("application/json;charset=UTF-8"); // 设置响应内容类型为JSON
        mapper.writeValue(response.getWriter(), Result.error(code, message)); // 将错误信息序列化为JSON并写入响应
    }
}
