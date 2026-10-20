package com.city.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
    @Select("SELECT * FROM coupon WHERE id=#{id} FOR UPDATE")
    Coupon selectForUpdate(@Param("id") Long id);

    @Update("UPDATE coupon SET claimed_quantity=claimed_quantity+1 WHERE id=#{id} AND status='ACTIVE' AND claimed_quantity<total_quantity AND starts_at<=#{now} AND ends_at>#{now}")
    int claimOne(@Param("id") Long id, @Param("now") java.time.LocalDateTime now);
}
