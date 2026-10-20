package com.city.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;



/**
 * 创建订单请求DTO类
 * 用于封装创建订单所需的请求数据
 * 包含票务类型ID和数量两个主要属性
 */
@Schema(description = "创建订单请求")  // Swagger注解，用于API文档生成
@Data  // Lombok注解，自动生成getter、setter等方法
public class CreateOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;  // 序列化版本ID

    @NotNull  // 验证注解，表示该字段不能为null
    private Long ticketTypeId;  // 票务类型ID

    @Min(1)  // 验证注解，表示该字段最小值为1
    private int quantity;

    private Long couponId;  // 购买数量
}
