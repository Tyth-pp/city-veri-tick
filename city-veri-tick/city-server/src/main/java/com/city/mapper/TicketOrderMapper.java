package com.city.mapper;

import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.entity.TicketOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface TicketOrderMapper extends BaseMapper<TicketOrder> {
    @Select("SELECT * FROM ticket_order WHERE user_id=#{userId} AND idempotency_key=#{key} LIMIT 1")
    TicketOrder selectByUserAndIdempotencyKey(@Param("userId") Long userId, @Param("key") String key);

    @Insert("INSERT INTO ticket_order(order_no,user_id,idempotency_key,coupon_id,real_name,id_card_hash,id_card_masked,ticket_type_id,quantity,amount,discount_amount,pay_amount,status,expired_at) "
            + "VALUES(#{orderNo},#{userId},#{idempotencyKey},#{couponId},#{realName},#{idCardHash},#{idCardMasked},#{ticketTypeId},#{quantity},#{amount},#{discountAmount},#{payAmount},#{status},#{expiredAt}) "
            + "ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertIdempotent(TicketOrder order);

    @Update("UPDATE ticket_order SET discount_amount=#{discount}, pay_amount=#{pay} WHERE id=#{orderId}")
    int updateAmounts(@Param("orderId") Long orderId, @Param("discount") java.math.BigDecimal discount, @Param("pay") java.math.BigDecimal pay);

    @Select("SELECT * FROM ticket_order WHERE id=#{orderId} FOR UPDATE")
    TicketOrder selectByIdForUpdate(@Param("orderId") Long orderId);

    @Update("UPDATE ticket_order SET status=#{next} WHERE id=#{orderId} AND status=#{previous}")
    int changeRefundStatus(@Param("orderId") Long orderId, @Param("previous") String previous, @Param("next") String next);

    @Update("UPDATE ticket_order SET status = 'PAID', paid_at = #{paidAt} "
            + "WHERE id = #{orderId} AND user_id = #{userId} AND status = 'PENDING_PAYMENT'")
    int markPaid(@Param("orderId") Long orderId, @Param("userId") Long userId,
                 @Param("paidAt") LocalDateTime paidAt);

    @Update("UPDATE ticket_order SET status = 'CANCELLED' "
            + "WHERE id = #{orderId} AND user_id = #{userId} AND status = 'PENDING_PAYMENT'")
    int markCancelled(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Update("UPDATE ticket_order SET status = 'EXPIRED' "
            + "WHERE id = #{orderId} AND status = 'PENDING_PAYMENT'")
    int markExpired(@Param("orderId") Long orderId);
}
