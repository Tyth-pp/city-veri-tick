package com.city.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.entity.UserCoupon;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
    @Select("SELECT COUNT(*) FROM user_coupon WHERE user_id=#{userId} AND coupon_id=#{couponId}")
    int countClaims(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Select("SELECT * FROM user_coupon WHERE user_id=#{userId} AND coupon_id=#{couponId} AND order_id=#{orderId} LIMIT 1 FOR UPDATE")
    UserCoupon findForOrder(@Param("userId") Long userId, @Param("couponId") Long couponId, @Param("orderId") Long orderId);

    @Update("UPDATE user_coupon SET status='EXPIRED' WHERE id=#{id} AND status='USED' AND order_id=#{orderId}")
    int forfeit(@Param("id") Long id, @Param("orderId") Long orderId);

    @Select("SELECT * FROM user_coupon WHERE user_id=#{userId} AND coupon_id=#{couponId} AND status='AVAILABLE' ORDER BY id LIMIT 1 FOR UPDATE")
    UserCoupon find(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Select("SELECT * FROM user_coupon WHERE id=#{id} AND user_id=#{userId} FOR UPDATE")
    UserCoupon selectForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_coupon SET status=#{status},order_id=#{orderId},locked_at=#{at} WHERE id=#{id} AND status=#{from}")
    int change(@Param("id") Long id, @Param("from") String from, @Param("status") String status, @Param("orderId") Long orderId, @Param("at") LocalDateTime at);

    @Update("UPDATE user_coupon SET status='USED',used_at=#{at} WHERE id=#{id} AND status='LOCKED' AND order_id=#{orderId}")
    int markUsed(@Param("id") Long id, @Param("orderId") Long orderId, @Param("at") LocalDateTime at);

    @Update("UPDATE user_coupon SET status='AVAILABLE',order_id=NULL,locked_at=NULL WHERE id=#{id} AND status='LOCKED' AND order_id=#{orderId}")
    int release(@Param("id") Long id, @Param("orderId") Long orderId);

    @Update("UPDATE user_coupon SET status='AVAILABLE',order_id=NULL,locked_at=NULL,used_at=NULL WHERE id=#{id} AND status='USED' AND order_id=#{orderId}")
    int restoreAfterRefund(@Param("id") Long id, @Param("orderId") Long orderId);
}
