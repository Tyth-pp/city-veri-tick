package com.city.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.entity.TicketType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TicketTypeMapper extends BaseMapper<TicketType> {
    @Update("UPDATE ticket_type SET stock=stock+#{quantity} WHERE id=#{ticketTypeId}")
    int restoreRefundedStock(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") int quantity);

    @Update("UPDATE ticket_type SET stock = stock - #{quantity}, locked_stock = locked_stock + #{quantity} "
            + "WHERE id = #{ticketTypeId} AND enabled = 1 AND stock >= #{quantity}")
    int lockStock(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") int quantity);

    @Update("UPDATE ticket_type SET locked_stock = locked_stock - #{quantity} "
            + "WHERE id = #{ticketTypeId} AND locked_stock >= #{quantity}")
    int confirmLockedStock(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") int quantity);

    @Update("UPDATE ticket_type SET stock = stock + #{quantity}, locked_stock = locked_stock - #{quantity} "
            + "WHERE id = #{ticketTypeId} AND locked_stock >= #{quantity}")
    int releaseLockedStock(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") int quantity);
}
