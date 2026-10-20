package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 优惠券分类范围实体类
 * 用于存储优惠券适用的商品分类信息
 */
@Data
@TableName("coupon_category_scope")
public class CouponCategoryScope implements Serializable {
    /**
     * 主键ID
     * 使用MyBatis-Plus的@TableId注解标记为主键
     */
    @TableId
    private Long id;
    /**
     * 优惠券ID
     * 关联优惠券表的主键，用于标识该分类范围属于哪个优惠券
     */
    private Long couponId;
    /**
     * 分类编码
     * 表示该优惠券适用的商品分类编码
     */
    private String categoryCode;
}
