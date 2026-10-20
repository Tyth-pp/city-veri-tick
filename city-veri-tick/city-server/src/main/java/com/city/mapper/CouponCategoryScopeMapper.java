package com.city.mapper;

import com.city.entity.CouponCategoryScope;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CouponCategoryScopeMapper extends BaseMapper<CouponCategoryScope> {
    @Select("SELECT COUNT(*) FROM coupon_category_scope WHERE coupon_id=#{couponId}")
    int count(@Param("couponId") Long couponId);

    @Select("SELECT category_code FROM coupon_category_scope WHERE coupon_id=#{couponId}")
    List<String> categories(@Param("couponId") Long couponId);
}
