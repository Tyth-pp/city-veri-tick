package com.city.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.city.enums.ActivityCategory;

import java.io.Serializable;

/**
 * 创建活动DTO（数据传输对象）
 * 用于封装创建活动所需的数据
 */
@Data
public class CreateActivityDTO implements Serializable {
    private static final long serialVersionUID = 1L;  // 序列化版本UID，用于确保类的版本兼容性

    @NotBlank  // 验证注解，表示该字段不能为空
    private String title;  // 活动标题
    @NotNull
    private ActivityCategory category;  // 活动分类
    @NotBlank
    private String content;  // 活动内容
    @NotBlank
    private String highlights;  // 活动亮点
    @NotBlank
    private String notices;  // 注意事项
    private String description;  // 旧数据兼容字段
    private Long coverFileId;  // 封面图片文件ID
    private String venueName;  // 活动场地名称
    private String venueAddress;  // 活动场地地址
}
