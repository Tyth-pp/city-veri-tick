package com.city.dto;

import com.city.constant.ActivityMessageConstant;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 创建活动场次的数据传输对象(DTO)
 * 用于接收和验证创建活动场次所需的请求数据
 */
@Data
public class CreateActivitySessionDTO implements Serializable {
    private static final long serialVersionUID = 1L;  // 序列化版本号，用于确保类的不同版本兼容
    /**
     * 活动开始时间
     * 必须字段，格式为 yyyy-MM-dd'T'HH:mm:ss
     * 示例值: 2030-01-01T10:00:00
     */
    @NotNull(message = ActivityMessageConstant.SESSION_START_REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2030-01-01T10:00:00")
    private LocalDateTime startTime;  // 活动开始时间
    /**
     * 活动结束时间
     * 必须字段，格式为 yyyy-MM-dd'T'HH:mm:ss
     * 示例值: 2030-01-01T12:00:00
     */
    @NotNull(message = ActivityMessageConstant.SESSION_END_REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(example = "2030-01-01T12:00:00")
    private LocalDateTime endTime;  // 活动结束时间
    /**
     * 活动容量
     * 必须字段，必须是大于0的整数
     */
    @NotNull(message = ActivityMessageConstant.SESSION_CAPACITY_REQUIRED)
    @Min(value = 1, message = ActivityMessageConstant.SESSION_CAPACITY_INVALID)
    private Integer capacity;  // 活动容量，即可参与人数
    /**
     * 销售开始时间
     * 可选字段，格式为 yyyy-MM-dd'T'HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime saleStartTime;
    /**
     * 销售结束时间
     * 可选字段，格式为 yyyy-MM-dd'T'HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime saleEndTime;
}
