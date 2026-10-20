package com.city.service.impl;
import com.city.entity.Coupon;
import com.city.mapper.CouponMapper;
import com.city.service.RefundService;

import com.city.dto.CreateOrderDTO;
import com.city.entity.Ticket;
import com.city.entity.TicketOrder;
import com.city.entity.TicketType;
import com.city.entity.Activity;
import com.city.entity.ActivitySession;
import com.city.entity.User;
import com.city.enums.ActivityStatus;
import com.city.enums.SessionStatus;
import com.city.enums.TicketStatus;
import com.city.exception.BusinessException;
import com.city.result.SystemResultCode;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivitySessionMapper;
import com.city.mapper.TicketMapper;
import com.city.mapper.TicketOrderMapper;
import com.city.mapper.TicketTypeMapper;
import com.city.vo.TicketViewVO;
import com.city.vo.OrderViewVO;
import com.city.service.OrderService;
import com.city.service.FileStorageService;
import com.city.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class OrderServiceImplTest {
    private final TicketTypeMapper ticketTypeMapper = mock(TicketTypeMapper.class);
    private final TicketOrderMapper ticketOrderMapper = mock(TicketOrderMapper.class);
    private final TicketMapper ticketMapper = mock(TicketMapper.class);
    private final ActivitySessionMapper activitySessionMapper = mock(ActivitySessionMapper.class);
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final OrderServiceImpl service = new OrderServiceImpl();

    @BeforeEach
    void setUp() {
        reset(ticketTypeMapper, ticketOrderMapper, ticketMapper, activitySessionMapper, activityMapper, userMapper, fileStorageService, passwordEncoder);
        when(ticketOrderMapper.selectByIdForUpdate(anyLong())).thenAnswer(invocation -> ticketOrderMapper.selectById(invocation.getArgument(0)));
        ReflectionTestUtils.setField(service, "ticketTypeMapper", ticketTypeMapper);
        ReflectionTestUtils.setField(service,"refundService",mock(RefundService.class));
        ReflectionTestUtils.setField(service, "ticketOrderMapper", ticketOrderMapper);
        ReflectionTestUtils.setField(service, "ticketMapper", ticketMapper);
        ReflectionTestUtils.setField(service, "activitySessionMapper", activitySessionMapper);
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "fileStorageService", fileStorageService);
        User user = new User(); user.setId(1L); user.setRealName("测试用户"); user.setIdCardHash("hash");
        when(userMapper.selectById(anyLong())).thenReturn(user);
        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(Instant.parse("2030-01-01T01:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsPendingOrderAndLocksStockWithoutIssuingTickets() {
        TicketType type = saleableTicketType();
        when(ticketTypeMapper.selectById(10L)).thenReturn(type);
        when(activitySessionMapper.selectById(20L)).thenReturn(onSaleSession());
        when(activityMapper.selectById(30L)).thenReturn(publishedActivity());
        when(ticketTypeMapper.lockStock(10L, 2)).thenReturn(1);
        doAnswer(invocation -> {
            invocation.<TicketOrder>getArgument(0).setId(101L);
            return 1;
        }).when(ticketOrderMapper).insert(any(TicketOrder.class));
        AtomicLong ticketId = new AtomicLong(201L);
        doAnswer(invocation -> {
            invocation.<Ticket>getArgument(0).setId(ticketId.getAndIncrement());
            return 1;
        }).when(ticketMapper).insert(any(Ticket.class));

        CreateOrderDTO request = new CreateOrderDTO();
        request.setTicketTypeId(10L);
        request.setQuantity(2);

        OrderViewVO result = service.create(7L, request);

        ArgumentCaptor<TicketOrder> orderCaptor = ArgumentCaptor.forClass(TicketOrder.class);
        verify(ticketOrderMapper).insert(orderCaptor.capture());
        TicketOrder order = orderCaptor.getValue();
        assertThat(order.getOrderNo()).hasSize(32);
        assertThat(order.getAmount()).isEqualByComparingTo("198.00");
        assertThat(order.getPayAmount()).isEqualByComparingTo("198.00");
        assertThat(order.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.getRealName()).isEqualTo("测试用户");
        assertThat(order.getIdCardHash()).isEqualTo("hash");
        assertThat(result.getOrderId()).isEqualTo(101L);
        assertThat(result.getTickets()).isNull();
        verify(ticketMapper, never()).insert(any(Ticket.class));
        verify(ticketTypeMapper).lockStock(10L, 2);
    }

    @Test
    void repeatedCreateWithSameIdempotencyKeyReturnsExistingOrderBeforeLockingStock() {
        TicketOrder existing=order(101L,7L,"PENDING_PAYMENT");
        when(ticketOrderMapper.selectByUserAndIdempotencyKey(7L,"checkout-1234")).thenReturn(existing);
        OrderViewVO result=service.create(7L,request(),"checkout-1234");
        assertThat(result.getOrderId()).isEqualTo(101L);
        verify(ticketTypeMapper,never()).lockStock(anyLong(),anyInt());
        verify(ticketOrderMapper,never()).insert(any(TicketOrder.class));
    }

    @Test
    void rejectsNonRealNameUserBeforeLockingStock() {
        when(ticketTypeMapper.selectById(10L)).thenReturn(saleableTicketType());
        when(activitySessionMapper.selectById(20L)).thenReturn(onSaleSession());
        when(activityMapper.selectById(30L)).thenReturn(publishedActivity());
        when(userMapper.selectById(7L)).thenReturn(new User());
        assertThatThrownBy(() -> service.create(7L, request()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.resultCode()).isEqualTo(SystemResultCode.REAL_NAME_REQUIRED));
        verify(ticketTypeMapper, never()).lockStock(anyLong(), anyInt());
        verify(ticketOrderMapper, never()).insert(any(TicketOrder.class));
    }

    @Test
    void rejectsEarlyPurchaseWithoutLockingStockEvenWhenUpcomingActivityIsVisible() {
        TicketType type = saleableTicketType();
        type.setSaleStartTime(LocalDateTime.of(2030, 1, 1, 2, 0));
        when(ticketTypeMapper.selectById(10L)).thenReturn(type);
        when(activitySessionMapper.selectById(20L)).thenReturn(onSaleSession());
        when(activityMapper.selectById(30L)).thenReturn(publishedActivity());
        assertConflict(type, SystemResultCode.TICKET_TYPE_NOT_ON_SALE);
        verify(ticketTypeMapper, never()).lockStock(anyLong(), anyInt());
    }

    @Test
    void rejectsDisabledTicketType() {
        TicketType type = saleableTicketType();
        type.setEnabled(false);
        when(ticketTypeMapper.selectById(10L)).thenReturn(type);

        assertConflict(type, SystemResultCode.TICKET_TYPE_NOT_ON_SALE);
    }

    @Test
    void rejectsSessionThatIsNotOnSale() {
        when(ticketTypeMapper.selectById(10L)).thenReturn(saleableTicketType());
        ActivitySession session = onSaleSession();
        session.setStatus(SessionStatus.DRAFT);
        when(activitySessionMapper.selectById(20L)).thenReturn(session);

        assertConflict(saleableTicketType(), SystemResultCode.SESSION_NOT_ON_SALE);
    }

    @Test
    void rejectsActivityThatIsNotPublished() {
        when(ticketTypeMapper.selectById(10L)).thenReturn(saleableTicketType());
        when(activitySessionMapper.selectById(20L)).thenReturn(onSaleSession());
        Activity activity = publishedActivity();
        activity.setStatus(ActivityStatus.OFFLINE);
        when(activityMapper.selectById(30L)).thenReturn(activity);

        assertConflict(saleableTicketType(), SystemResultCode.ACTIVITY_NOT_PUBLISHED);
    }

    @Test
    void rejectsOrderOutsideSaleWindow() {
        TicketType type = saleableTicketType();
        type.setSaleEndTime(LocalDateTime.of(2030, 1, 1, 0, 30));
        when(ticketTypeMapper.selectById(10L)).thenReturn(type);
        when(activitySessionMapper.selectById(20L)).thenReturn(onSaleSession());
        when(activityMapper.selectById(30L)).thenReturn(publishedActivity());

        assertConflict(type, SystemResultCode.TICKET_TYPE_NOT_ON_SALE);
    }

    @Test
    void rejectsOrderWhenAtomicStockDeductionFails() {
        when(ticketTypeMapper.selectById(10L)).thenReturn(saleableTicketType());
        when(activitySessionMapper.selectById(20L)).thenReturn(onSaleSession());
        when(activityMapper.selectById(30L)).thenReturn(publishedActivity());
        when(ticketTypeMapper.lockStock(10L, 2)).thenReturn(0);

        assertThatThrownBy(() -> service.create(7L, request()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.CONFLICT));
        verify(ticketOrderMapper, never()).insert(any(TicketOrder.class));
    }

    @Test
    void paysPendingOrderAndIssuesTickets() {
        TicketOrder order = order(101L, 7L, "PENDING_PAYMENT");
        when(ticketOrderMapper.selectById(101L)).thenReturn(order);
        when(ticketTypeMapper.confirmLockedStock(10L, 1)).thenReturn(1);
        when(ticketTypeMapper.selectById(10L)).thenReturn(saleableTicketType());
        when(activitySessionMapper.selectById(20L)).thenReturn(onSaleSession());
        when(activityMapper.selectById(30L)).thenReturn(publishedActivity());
        when(ticketOrderMapper.markPaid(eq(101L), eq(7L), any(LocalDateTime.class))).thenReturn(1);
        doAnswer(invocation -> { invocation.<Ticket>getArgument(0).setId(201L); return 1; })
                .when(ticketMapper).insert(any(Ticket.class));

        OrderViewVO result = service.pay(7L, 101L);

        assertThat(result.getStatus()).isEqualTo("PAID");
        verify(ticketTypeMapper).confirmLockedStock(10L, 1);
        verify(ticketMapper).insert(any(Ticket.class));
    }

    @Test
    void cancelsPendingOrderAndReleasesStock() {
        TicketOrder order = order(101L, 7L, "PENDING_PAYMENT");
        when(ticketOrderMapper.selectById(101L)).thenReturn(order);
        when(ticketOrderMapper.markCancelled(101L, 7L)).thenReturn(1);
        when(ticketTypeMapper.releaseLockedStock(10L, 1)).thenReturn(1);

        service.cancel(7L, 101L);

        verify(ticketTypeMapper).releaseLockedStock(10L, 1);
        verify(ticketOrderMapper).markCancelled(101L, 7L);
    }

    @Test
    void expiresPendingOrdersAndReleasesStock() {
        TicketOrder order = order(101L, 7L, "PENDING_PAYMENT");
        order.setExpiredAt(LocalDateTime.of(2029, 12, 31, 23, 59));
        when(ticketOrderMapper.selectList(any())).thenReturn(List.of(order));
        when(ticketOrderMapper.markExpired(101L)).thenReturn(1);
        when(ticketTypeMapper.releaseLockedStock(10L, 1)).thenReturn(1);

        int count = service.expirePendingOrders();

        assertThat(count).isEqualTo(1);
        verify(ticketTypeMapper).releaseLockedStock(10L, 1);
    }

    @Test
    void listsOnlyCurrentUsersOrdersWithOptionalStatusFilter() {
        TicketOrder first = order(101L, 7L, "PAID");
        TicketOrder second = order(102L, 8L, "CANCELLED");
        when(ticketOrderMapper.selectList(any())).thenReturn(List.of(first));

        List<OrderViewVO> result = service.list(7L, "PAID");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo(101L);
        verify(ticketOrderMapper).selectList(any());
    }

    @Test
    void returnsOrderDetailOnlyForCurrentUser() {
        TicketOrder order = order(101L, 7L, "PAID");
        Ticket ticket = new Ticket();
        ticket.setId(201L);
        ticket.setOrderId(101L);
        ticket.setVerificationCode("verify-201");
        ticket.setStatus(TicketStatus.UNUSED);
        when(ticketOrderMapper.selectById(101L)).thenReturn(order);
        when(ticketTypeMapper.selectById(10L)).thenReturn(saleableTicketType());
        when(activitySessionMapper.selectById(20L)).thenReturn(onSaleSession());
        when(activityMapper.selectById(30L)).thenReturn(publishedActivity());
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket));

        OrderViewVO result = service.detail(7L, 101L);

        assertThat(result.getOrderId()).isEqualTo(101L);
        assertThat(result.getTickets()).extracting(TicketViewVO::getTicketId).containsExactly(201L);
    }

    @Test
    void rejectsDetailRequestForAnotherUser() {
        when(ticketOrderMapper.selectById(101L)).thenReturn(order(101L, 8L, "PAID"));

        assertThatThrownBy(() -> service.detail(7L, 101L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.NOT_FOUND));
    }

    @Test
    void orderListProvidesRealVenueCoverAndTicketStatusesForWallet() {
        Activity activity = publishedActivity();
        activity.setVenueName("江南大剧院");
        activity.setCoverFileId(40L);
        Ticket ticket = new Ticket();
        ticket.setId(201L);
        ticket.setOrderId(101L);
        ticket.setStatus(TicketStatus.UNUSED);
        when(ticketOrderMapper.selectList(any())).thenReturn(List.of(order(101L, 7L, "PAID")));
        when(ticketTypeMapper.selectById(10L)).thenReturn(saleableTicketType());
        when(activitySessionMapper.selectById(20L)).thenReturn(onSaleSession());
        when(activityMapper.selectById(30L)).thenReturn(activity);
        when(fileStorageService.getUrl(40L)).thenReturn("/uploads/real-cover.jpg");
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket));

        OrderViewVO result = service.list(7L, null).get(0);

        assertThat(result.getVenueName()).isEqualTo("江南大剧院");
        assertThat(result.getCoverFileUrl()).isEqualTo("/uploads/real-cover.jpg");
        assertThat(result.getTickets()).extracting(TicketViewVO::getStatus).containsExactly("UNUSED");
    }

    @Test
    void refusesToCreateOrderAfterSessionEndsBeforeSalesWindowCloses() {
        ActivitySession ended = onSaleSession();
        ended.setEndTime(LocalDateTime.of(2030, 1, 1, 0, 59));
        when(ticketTypeMapper.selectById(10L)).thenReturn(saleableTicketType());
        when(activitySessionMapper.selectById(20L)).thenReturn(ended);
        when(activityMapper.selectById(30L)).thenReturn(publishedActivity());
        assertThatThrownBy(() -> service.create(7L, request()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.SESSION_NOT_ON_SALE));
        verify(ticketTypeMapper, never()).lockStock(anyLong(), anyInt());
        verify(ticketOrderMapper, never()).insert(any(TicketOrder.class));
    }

    private void assertConflict(TicketType ignored, SystemResultCode expected) {
        assertThatThrownBy(() -> service.create(7L, request()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(expected));
        verify(ticketOrderMapper, never()).insert(any(TicketOrder.class));
    }

    private CreateOrderDTO request() {
        CreateOrderDTO request = new CreateOrderDTO();
        request.setTicketTypeId(10L);
        request.setQuantity(2);
        return request;
    }

    private TicketOrder order(Long id, Long userId, String status) {
        TicketOrder order = new TicketOrder();
        order.setIdCardHash("hash");
        order.setId(id);
        order.setOrderNo("order-" + id);
        order.setUserId(userId);
        order.setTicketTypeId(10L);
        order.setQuantity(1);
        order.setAmount(new BigDecimal("99.00"));
        order.setPayAmount(order.getAmount());
        order.setStatus(status);
        return order;
    }

    private TicketType saleableTicketType() {
        TicketType type = new TicketType();
        type.setId(10L);
        type.setSessionId(20L);
        type.setPrice(new BigDecimal("99.00"));
        type.setStock(100);
        type.setEnabled(true);
        type.setSaleStartTime(LocalDateTime.of(2029, 12, 1, 0, 0));
        type.setSaleEndTime(LocalDateTime.of(2030, 1, 1, 9, 30));
        return type;
    }

    private ActivitySession onSaleSession() {
        ActivitySession session = new ActivitySession();
        session.setId(20L);
        session.setActivityId(30L);
        session.setStatus(SessionStatus.ON_SALE);
        session.setEndTime(LocalDateTime.of(2030, 1, 2, 0, 0));
        return session;
    }

    private Activity publishedActivity() {
        Activity activity = new Activity();
        activity.setId(30L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        return activity;
    }

    @Test void orderDetailIncludesActualCouponNameAndStoredDiscount() {
        var couponMapper=mock(CouponMapper.class);ReflectionTestUtils.setField(service,"couponMapper",couponMapper);
        TicketOrder value=new TicketOrder();value.setId(101L);value.setUserId(7L);value.setCouponId(9L);value.setAmount(new BigDecimal("100"));value.setDiscountAmount(new BigDecimal("20"));value.setPayAmount(new BigDecimal("80"));
        when(ticketOrderMapper.selectById(101L)).thenReturn(value);
        Coupon coupon=new Coupon();coupon.setName("真实满减券");when(couponMapper.selectById(9L)).thenReturn(coupon);
        OrderViewVO result=service.detail(7L,101L);
        assertThat(result.getCouponName()).isEqualTo("真实满减券");assertThat(result.getDiscountAmount()).isEqualByComparingTo("20");assertThat(result.getPayAmount()).isEqualByComparingTo("80");
    }

    @Test void paymentPolicyAndOrdersUseTheSameDurationAndServerClock() {
        assertThat(service.paymentPolicy().getPaymentTimeoutSeconds()).isEqualTo(900);
        assertThat(service.paymentPolicy().getServerTime()).isEqualTo(Instant.parse("2030-01-01T01:00:00Z"));
        TicketOrder value=order(101L,7L,"PENDING_PAYMENT");when(ticketOrderMapper.selectById(101L)).thenReturn(value);
        assertThat(service.detail(7L,101L).getServerTime()).isEqualTo(service.paymentPolicy().getServerTime());
    }
    @Test void synchronizesExpiryOnlyAfterDeadlineAndDoesNotClosePaidOrder() {
        TicketOrder value=order(101L,7L,"PENDING_PAYMENT");value.setExpiredAt(LocalDateTime.of(2030,1,1,1,1));when(ticketOrderMapper.selectById(101L)).thenReturn(value);
        assertThat(service.synchronizeExpiry(7L,101L).getStatus()).isEqualTo("PENDING_PAYMENT");verify(ticketOrderMapper,never()).markExpired(anyLong());
        value.setExpiredAt(LocalDateTime.of(2030,1,1,1,0));when(ticketOrderMapper.markExpired(101L)).thenReturn(1);when(ticketTypeMapper.releaseLockedStock(10L,1)).thenReturn(1);
        assertThat(service.synchronizeExpiry(7L,101L).getStatus()).isEqualTo("EXPIRED");
        service.synchronizeExpiry(7L,101L);verify(ticketTypeMapper,times(1)).releaseLockedStock(10L,1);
        value.setStatus("PAID");assertThat(service.synchronizeExpiry(7L,101L).getStatus()).isEqualTo("PAID");
    }
    @Test void rejectsPaymentAtTheExactDeadlineWithoutIssuingTickets() {
        TicketOrder value=order(101L,7L,"PENDING_PAYMENT");value.setExpiredAt(LocalDateTime.of(2030,1,1,1,0));when(ticketOrderMapper.selectById(101L)).thenReturn(value);
        assertThatThrownBy(()->service.pay(7L,101L)).hasMessageContaining("超时");
        verify(ticketTypeMapper,never()).confirmLockedStock(anyLong(),anyInt());verify(ticketMapper,never()).insert(any(Ticket.class));
    }
    @Test void expirySynchronizationRejectsOtherAccountsOrder() {
        when(ticketOrderMapper.selectById(101L)).thenReturn(order(101L,8L,"PENDING_PAYMENT"));
        assertThatThrownBy(()->service.synchronizeExpiry(7L,101L)).isInstanceOf(BusinessException.class);verify(ticketOrderMapper,never()).markExpired(anyLong());
    }
}
