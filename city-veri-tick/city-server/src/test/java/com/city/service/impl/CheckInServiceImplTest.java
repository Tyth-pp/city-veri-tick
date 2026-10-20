package com.city.service.impl;
import java.util.List;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.city.result.SystemResultCode;
import com.city.entity.*;
import com.city.enums.TicketStatus;
import com.city.exception.BusinessException;
import com.city.mapper.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CheckInServiceImplTest {
    private final CheckInServiceImpl service = new CheckInServiceImpl();
    private final TicketMapper tickets = mock(TicketMapper.class);
    private final CheckInRecordMapper records = mock(CheckInRecordMapper.class);
    private final TicketOrderMapper orders = mock(TicketOrderMapper.class);
    private final ActivityVerifierMapper verifiers = mock(ActivityVerifierMapper.class);
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        for (Class<?> entity : new Class<?>[]{Ticket.class, ActivityVerifier.class}) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entity);
        }
        ReflectionTestUtils.setField(service, "ticketMapper", tickets);
        ReflectionTestUtils.setField(service, "checkInRecordMapper", records);
        ReflectionTestUtils.setField(service, "orderMapper", orders);
        ReflectionTestUtils.setField(service, "verifierMapper", verifiers);
        ReflectionTestUtils.setField(service, "passwordEncoder", new BCryptPasswordEncoder());
        TicketTypeMapper types = mock(TicketTypeMapper.class);
        ActivitySessionMapper sessions = mock(ActivitySessionMapper.class);
        ActivityMapper activities = mock(ActivityMapper.class);
        ReflectionTestUtils.setField(service, "ticketTypeMapper", types);
        ReflectionTestUtils.setField(service, "sessionMapper", sessions);
        ReflectionTestUtils.setField(service, "activityMapper", activities);
        ticket = new Ticket(); ticket.setId(1L); ticket.setOrderId(2L); ticket.setTicketTypeId(3L); ticket.setStatus(TicketStatus.UNUSED);
        TicketType type = new TicketType(); type.setSessionId(4L);
        ActivitySession session = new ActivitySession(); session.setActivityId(5L);
        Activity activity = new Activity(); activity.setId(5L);
        when(tickets.selectOne(any())).thenReturn(ticket);
        when(types.selectById(3L)).thenReturn(type);
        when(sessions.selectById(4L)).thenReturn(session);
        when(activities.selectById(5L)).thenReturn(activity);
        TicketOrder order = new TicketOrder(); order.setRealName("测试用户"); order.setIdCardMasked("110***********001X");
        order.setIdCardHash(new BCryptPasswordEncoder().encode("11010119900101001X"));
        when(orders.selectById(2L)).thenReturn(order);
        order.setId(2L);order.setStatus("PAID");
        when(orders.selectByIdForUpdate(2L)).thenReturn(order);
        when(tickets.selectForRefund(2L)).thenReturn(List.of(ticket));
        when(verifiers.selectCount(any())).thenReturn(1L);
        when(tickets.update(isNull(), any())).thenReturn(1);
    }

    @Test
    void refundInProgressCannotBeCheckedIn() {
        TicketOrder order = orders.selectById(2L);
        order.setStatus("REFUNDING");
        assertThatThrownBy(() -> service.checkIn(9L, "code", "11010119900101001X"))
                .hasMessageContaining("退款");
        verify(tickets, never()).update(isNull(), any());
        verify(records, never()).insert(any(CheckInRecord.class));
    }

    @Test void staleUnusedReadCannotCheckInTicketAfterAnotherCheckInCommits() {
        Ticket fresh=new Ticket();fresh.setId(ticket.getId());fresh.setStatus(TicketStatus.CHECKED_IN);
        when(tickets.selectForRefund(2L)).thenReturn(List.of(fresh));
        assertThatThrownBy(()->service.checkIn(9L,"code","11010119900101001X")).isInstanceOf(BusinessException.class);
        verify(records,never()).insert(any(CheckInRecord.class));
    }

    @Test void refundedOrderCannotBeCheckedInEvenWithPreviouslyUnusedTicket() {
        orders.selectById(2L).setStatus("REFUNDED");
        assertThatThrownBy(()->service.checkIn(9L,"code","11010119900101001X")).hasMessageContaining("退款");
        verify(records,never()).insert(any(CheckInRecord.class));
    }

    @Test
    void mismatchedIdentityDoesNotConsumeTicket() {
        assertThatThrownBy(() -> service.checkIn(9L, "code", "110101199001010020"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.resultCode()).isEqualTo(SystemResultCode.ID_CARD_MISMATCH));
        verify(tickets, never()).update(isNull(), any());
        verify(records, never()).insert(any(CheckInRecord.class));
    }

    @Test
    void matchingIdentityChecksInAndReturnsMaskedIdentity() {
        var view = service.checkIn(9L, "code", "11010119900101001x");
        assertThat(view.getTicketStatus()).isEqualTo("CHECKED_IN");
        assertThat(view.getIdCardMasked()).isEqualTo("110***********001X");
        verify(records).insert(any(CheckInRecord.class));
    }

    @Test
    void repeatedCheckInDoesNotInsertAnotherRecord() {
        ticket.setStatus(TicketStatus.CHECKED_IN);
        assertThatThrownBy(() -> service.checkIn(9L, "code", "11010119900101001X"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.resultCode()).isEqualTo(SystemResultCode.TICKET_ALREADY_CHECKED_IN));
        verify(records, never()).insert(any(CheckInRecord.class));
    }

    @Test
    void unassignedVerifierCannotCheckIn() {
        when(verifiers.selectCount(any())).thenReturn(0L);
        assertThatThrownBy(() -> service.checkIn(9L, "code", "11010119900101001X"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.resultCode()).isEqualTo(SystemResultCode.FORBIDDEN));
        verify(tickets, never()).update(isNull(), any());
    }
}
