package com.city.service.impl;
import com.city.service.ManagementMessageService;
import com.city.service.RefundService;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.city.entity.*;
import com.city.enums.TicketStatus;
import com.city.mapper.*;
import com.city.service.OrganizerAccessService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefundServiceTest {
    private final RefundService service = new RefundServiceImpl();
    private final TicketOrderMapper orders = mock(TicketOrderMapper.class);
    private final TicketMapper tickets = mock(TicketMapper.class);
    private final TicketTypeMapper types = mock(TicketTypeMapper.class);
    private final ActivitySessionMapper sessions = mock(ActivitySessionMapper.class);
    private final ActivityMapper activities = mock(ActivityMapper.class);
    private final RefundRequestMapper refunds = mock(RefundRequestMapper.class);
    private final OrganizerAccessService access = mock(OrganizerAccessService.class);
    private final UserMapper users = mock(UserMapper.class);
    private TicketOrder order; private RefundRequest refund; private ActivitySession session; private Ticket ticket;
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service,"messages",mock(ManagementMessageService.class));
        for(Class<?> entity:List.of(Ticket.class,TicketOrder.class,RefundRequest.class))
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"refund"),entity);
        ReflectionTestUtils.setField(service,"orders",orders); ReflectionTestUtils.setField(service,"tickets",tickets);
        ReflectionTestUtils.setField(service,"types",types);ReflectionTestUtils.setField(service,"sessions",sessions);
        ReflectionTestUtils.setField(service,"activities",activities);ReflectionTestUtils.setField(service,"refunds",refunds);
        ReflectionTestUtils.setField(service,"access",access);ReflectionTestUtils.setField(service,"users",users);
        ReflectionTestUtils.setField(service,"clock",Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"),ZoneId.of("Asia/Shanghai")));
        order=new TicketOrder();order.setId(10L);order.setUserId(1L);order.setTicketTypeId(20L);order.setQuantity(1);order.setStatus("PAID");order.setPayAmount(new BigDecimal("88.00"));order.setAmount(new BigDecimal("98.00"));
        when(orders.selectByIdForUpdate(10L)).thenReturn(order);
        TicketType type=new TicketType();type.setId(20L);type.setSessionId(30L);when(types.selectById(20L)).thenReturn(type);
        session=new ActivitySession();session.setActivityId(40L);session.setStartTime(LocalDateTime.of(2026,10,18,8,0));when(sessions.selectById(30L)).thenReturn(session);
        Activity activity=new Activity();activity.setOrganizerId(50L);when(activities.selectById(40L)).thenReturn(activity);
        when(access.requireOrganizerId(2L)).thenReturn(50L);
        ticket=new Ticket();ticket.setId(60L);ticket.setOrderId(10L);ticket.setStatus(TicketStatus.UNUSED);when(tickets.selectForRefund(10L)).thenReturn(List.of(ticket));
        when(orders.changeRefundStatus(anyLong(),anyString(),anyString())).thenReturn(1);
        when(refunds.insert(any(RefundRequest.class))).thenAnswer(inv->{inv.getArgument(0,RefundRequest.class).setId(70L);return 1;});
        when(refunds.updateById(any(RefundRequest.class))).thenReturn(1);when(types.restoreRefundedStock(20L,1)).thenReturn(1);
        when(tickets.refundUnused(10L)).thenReturn(1);
        refund=new RefundRequest();refund.setId(70L);refund.setOrderId(10L);refund.setUserId(1L);refund.setStatus("PENDING");refund.setAmount(new BigDecimal("88.00"));
        when(refunds.selectById(70L)).thenReturn(refund);
        User reviewer=new User();reviewer.setStaffName("审核人李");reviewer.setUsername("organizer_li");when(users.selectById(2L)).thenReturn(reviewer);
    }
    @Test void applicantMustOwnOrder(){assertThatThrownBy(()->service.apply(9L,10L,"行程变化")).hasMessageContaining("订单不存在");verify(refunds,never()).insert(any(RefundRequest.class));}
    @Test void applicationUsesActualPaidAmountAndLocksOrderBeforeTickets(){var r=service.apply(1L,10L," 行程变化 ");assertThat(r.getAmount()).isEqualByComparingTo("88");assertThat(r.getReason()).isEqualTo("行程变化");assertThat(r.getStatus()).isEqualTo("PENDING");var sequence=inOrder(orders,tickets);sequence.verify(orders).selectByIdForUpdate(10L);sequence.verify(tickets).selectForRefund(10L);verify(orders).changeRefundStatus(10L,"PAID","REFUNDING");}
    @Test void lessThan48HoursCannotApply(){session.setStartTime(LocalDateTime.of(2026,10,17,7,59));assertThatThrownBy(()->service.apply(1L,10L,"行程变化")).hasMessageContaining("48");verify(refunds,never()).insert(any(RefundRequest.class));}
    @Test void exact48HoursCanApply(){session.setStartTime(LocalDateTime.of(2026,10,17,8,0));assertThat(service.apply(1L,10L,"行程变化").getStatus()).isEqualTo("PENDING");}
    @Test void checkedInTicketPreventsWholeOrderRefund(){ticket.setStatus(TicketStatus.CHECKED_IN);assertThatThrownBy(()->service.apply(1L,10L,"行程变化")).hasMessageContaining("核销");verify(refunds,never()).insert(any(RefundRequest.class));}
    @Test void duplicatePendingApplicationIsRejected(){order.setStatus("REFUNDING");assertThatThrownBy(()->service.apply(1L,10L,"重复申请")).hasMessageContaining("处理中");verify(refunds,never()).insert(any(RefundRequest.class));}
    @Test void emptyReasonCannotApply(){assertThatThrownBy(()->service.apply(1L,10L,"  ")).hasMessageContaining("原因");verify(refunds,never()).insert(any(RefundRequest.class));}
    @Test void approvalInvalidatesTicketsRestoresInventoryAndRecordsReviewer(){order.setStatus("REFUNDING");var r=service.review(2L,10L,70L,true,"同意");assertThat(r.getStatus()).isEqualTo("REFUNDED");assertThat(r.getAmount()).isEqualByComparingTo("88");assertThat(r.getReviewerName()).isEqualTo("审核人李");assertThat(r.getRefundNo()).startsWith("SIM");assertThat(r.getProcessedAt()).isNotNull();verify(tickets).refundUnused(10L);verify(types).restoreRefundedStock(20L,1);verify(orders).changeRefundStatus(10L,"REFUNDING","REFUNDED");}
    @Test void repeatedApprovalCannotRestoreInventoryAgain(){order.setStatus("REFUNDED");refund.setStatus("REFUNDED");assertThatThrownBy(()->service.review(2L,10L,70L,true,"同意")).hasMessageContaining("处理");verify(types,never()).restoreRefundedStock(anyLong(),anyInt());}
    @Test void rejectionRequiresReasonAndRestoresUsabilityWithoutStockChange(){order.setStatus("REFUNDING");assertThatThrownBy(()->service.review(2L,10L,70L,false," ")).hasMessageContaining("拒绝原因");var r=service.review(2L,10L,70L,false,"不符合活动安排");assertThat(r.getStatus()).isEqualTo("REJECTED");verify(orders).changeRefundStatus(10L,"REFUNDING","PAID");verify(tickets,never()).refundUnused(anyLong());verify(types,never()).restoreRefundedStock(anyLong(),anyInt());}
    @Test void otherOrganizerCannotReview(){order.setStatus("REFUNDING");when(access.requireOrganizerId(2L)).thenReturn(99L);assertThatThrownBy(()->service.review(2L,10L,70L,true,"同意")).hasMessageContaining("订单不存在");verify(refunds,never()).updateById(any(RefundRequest.class));}
    @Test void partialTicketUpdateAbortsRefund(){order.setStatus("REFUNDING");when(tickets.refundUnused(10L)).thenReturn(0);assertThatThrownBy(()->service.review(2L,10L,70L,true,"同意")).hasMessageContaining("票券");verify(types,never()).restoreRefundedStock(anyLong(),anyInt());}
}
