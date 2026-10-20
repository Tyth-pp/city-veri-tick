package com.city.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
    @Select("SELECT * FROM ticket WHERE order_id=#{orderId} ORDER BY id FOR UPDATE")
    List<Ticket> selectForRefund(@Param("orderId") Long orderId);

    @Update("UPDATE ticket SET status='REFUNDED' WHERE order_id=#{orderId} AND status='UNUSED'")
    int refundUnused(@Param("orderId") Long orderId);
}
