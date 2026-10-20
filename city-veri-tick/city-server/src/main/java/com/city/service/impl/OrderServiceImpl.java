package com.city.service.impl;

import com.city.constant.OrderMessageConstant;
import com.city.entity.Coupon;
import com.city.mapper.CouponMapper;
import com.city.service.CouponService;
import com.city.vo.OrderPaymentPolicyVO;
import com.city.service.RefundService;
import com.city.entity.User;
import com.city.mapper.UserMapper;

import com.city.result.SystemResultCode;
import com.city.dto.CreateOrderDTO;
import com.city.entity.Ticket;
import com.city.entity.TicketOrder;
import com.city.entity.TicketType;
import com.city.entity.Activity;
import com.city.entity.ActivitySession;
import com.city.enums.ActivityStatus;
import com.city.enums.SessionStatus;
import com.city.enums.TicketStatus;
import com.city.exception.BusinessException;
import com.city.mapper.TicketMapper;
import com.city.mapper.TicketOrderMapper;
import com.city.mapper.TicketTypeMapper;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivitySessionMapper;
import com.city.service.OrderService;
import com.city.service.FileStorageService;
import com.city.vo.TicketViewVO;
import com.city.vo.OrderViewVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.MessageDigest;

@Service
public class OrderServiceImpl implements OrderService {
    private static final java.time.Duration PAYMENT_WINDOW = java.time.Duration.ofMinutes(15);
    @Autowired
    private RefundService refundService;
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private TicketTypeMapper ticketTypeMapper;
    @Autowired
    private TicketOrderMapper ticketOrderMapper;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private ActivitySessionMapper activitySessionMapper;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private Clock clock;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private UserMapper userMapper;
    @Autowired(required = false)
    private StringRedisTemplate redis;
    @Autowired(required = false)
    private CouponService couponService;

    @Override
    @Transactional
    public OrderViewVO create(Long userId, CreateOrderDTO createOrderDTO) {
        return create(userId, createOrderDTO, null);
    }

    @Override
    @Transactional
    public OrderViewVO create(Long userId, CreateOrderDTO createOrderDTO, String requestKey) {
        String idempotencyKey = normalizeIdempotencyKey(requestKey);
        if (idempotencyKey != null) {
            TicketOrder existing = ticketOrderMapper.selectByUserAndIdempotencyKey(userId, idempotencyKey);
            if (existing != null) return toView(existing, false);
            if (!tryAcquireCreateLock(userId, idempotencyKey)) {
                existing = ticketOrderMapper.selectByUserAndIdempotencyKey(userId, idempotencyKey);
                if (existing != null) return toView(existing, false);
                throw new BusinessException(SystemResultCode.ORDER_REQUEST_BUSY);
            }
        }
        TicketType type = ticketTypeMapper.selectById(createOrderDTO.getTicketTypeId());
        if (type == null) {
            throw new BusinessException(SystemResultCode.BAD_REQUEST);
        }
        validateSaleable(type, createOrderDTO.getQuantity());
        User user = userMapper.selectById(userId);
        if (user == null || user.getRealName() == null || user.getIdCardHash() == null) {
            throw new BusinessException(SystemResultCode.REAL_NAME_REQUIRED);
        }
        if (ticketTypeMapper.lockStock(type.getId(), createOrderDTO.getQuantity()) != 1) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        TicketOrder order = new TicketOrder();
        order.setOrderNo(generateBusinessNo());
        order.setUserId(userId);
        order.setIdempotencyKey(idempotencyKey);
        order.setCouponId(createOrderDTO.getCouponId());
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setRealName(user.getRealName());
        order.setIdCardHash(user.getIdCardHash());
        order.setIdCardMasked(user.getIdCardMasked());
        order.setTicketTypeId(type.getId());
        order.setQuantity(createOrderDTO.getQuantity());
        order.setAmount(type.getPrice().multiply(BigDecimal.valueOf(createOrderDTO.getQuantity())));
        order.setPayAmount(order.getAmount());
        order.setStatus("PENDING_PAYMENT");
        order.setExpiredAt(LocalDateTime.now(clock).plus(PAYMENT_WINDOW));
        if (idempotencyKey == null) ticketOrderMapper.insert(order);
        else {
            ticketOrderMapper.insertIdempotent(order);
            TicketOrder persisted = ticketOrderMapper.selectByUserAndIdempotencyKey(userId, idempotencyKey);
            if (persisted != null && !order.getOrderNo().equals(persisted.getOrderNo())) {
                ticketTypeMapper.releaseLockedStock(order.getTicketTypeId(), order.getQuantity());
                return toView(persisted, false);
            }
        }
        if (order.getCouponId() != null && couponService != null) {
            BigDecimal discount = couponService.lockForOrder(userId, order.getCouponId(), order.getId(), order.getAmount(), activityCategory(type), activitySessionMapper.selectById(type.getSessionId()).getActivityId());
            order.setDiscountAmount(discount == null ? BigDecimal.ZERO : discount);
            order.setPayAmount(order.getAmount().subtract(order.getDiscountAmount()));
            ticketOrderMapper.updateAmounts(order.getId(), order.getDiscountAmount(), order.getPayAmount());
        }
        return toView(order, false);
    }

    @Override
    @Transactional
    public OrderViewVO pay(Long userId, Long orderId) {
        TicketOrder order = ticketOrderMapper.selectByIdForUpdate(orderId);
        if (order == null || !userId.equals(order.getUserId())) throw new BusinessException(SystemResultCode.NOT_FOUND);
        if ("PAID".equals(order.getStatus())) return toView(order, true);
        if (order.getIdCardHash() == null) throw new BusinessException(SystemResultCode.REAL_NAME_REQUIRED);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        if (order.getExpiredAt() != null && !order.getExpiredAt().isAfter(LocalDateTime.now(clock))) {
            // 拒绝超时支付；关闭订单由独立的事务入口或定时任务完成，避免异常回滚释放操作。
            throw new BusinessException(SystemResultCode.ORDER_PAYMENT_EXPIRED);
        }
        if (ticketTypeMapper.confirmLockedStock(order.getTicketTypeId(), order.getQuantity()) != 1) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        if (ticketOrderMapper.markPaid(orderId, userId, LocalDateTime.now(clock)) != 1) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now(clock));
        if (order.getCouponId() != null && couponService != null)
            couponService.use(userId, order.getCouponId(), orderId);
        issueTickets(order);
        return toView(order, true);
    }

    @Override
    public OrderPaymentPolicyVO paymentPolicy() {
        return new OrderPaymentPolicyVO(PAYMENT_WINDOW.getSeconds(), clock.instant());
    }

    @Override
    @Transactional
    public OrderViewVO synchronizeExpiry(Long userId, Long orderId) {
        TicketOrder order = ticketOrderMapper.selectByIdForUpdate(orderId);
        if (order == null || !userId.equals(order.getUserId())) throw new BusinessException(SystemResultCode.NOT_FOUND);
        if ("PENDING_PAYMENT".equals(order.getStatus()) && order.getExpiredAt() != null
                && !order.getExpiredAt().isAfter(LocalDateTime.now(clock))) expire(order);
        return toView(order, true);
    }

    @Override
    @Transactional
    public OrderViewVO cancel(Long userId, Long orderId) {
        TicketOrder order = ownedOrder(userId, orderId);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        if (ticketOrderMapper.markCancelled(orderId, userId) != 1) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        if (ticketTypeMapper.releaseLockedStock(order.getTicketTypeId(), order.getQuantity()) != 1) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        if (order.getCouponId() != null && couponService != null)
            couponService.release(userId, order.getCouponId(), orderId);
        order.setStatus("CANCELLED");
        return toView(order, false);
    }

    @Override
    @Scheduled(fixedDelayString = "60000")
    @Transactional
    public int expirePendingOrders() {
        List<TicketOrder> orders = ticketOrderMapper.selectList(Wrappers.<TicketOrder>lambdaQuery()
                .eq(TicketOrder::getStatus, "PENDING_PAYMENT")
                .le(TicketOrder::getExpiredAt, LocalDateTime.now(clock)));
        orders.forEach(this::expire);
        return orders.size();
    }

    private void issueTickets(TicketOrder order) {
        for (int i = 0; i < order.getQuantity(); i++) {
            Ticket ticket = new Ticket();
            ticket.setTicketNo(generateBusinessNo());
            ticket.setOrderId(order.getId());
            ticket.setTicketTypeId(order.getTicketTypeId());
            ticket.setStatus(TicketStatus.UNUSED);
            ticket.setVerificationCode(UUID.randomUUID().toString());
            ticketMapper.insert(ticket);
        }
    }

    private TicketOrder ownedOrder(Long userId, Long orderId) {
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(SystemResultCode.NOT_FOUND);
        }
        return order;
    }

    private void expire(TicketOrder order) {
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            return;
        }
        if (ticketOrderMapper.markExpired(order.getId()) != 1) {
            return;
        }
        if (ticketTypeMapper.releaseLockedStock(order.getTicketTypeId(), order.getQuantity()) != 1) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        if (order.getCouponId() != null && couponService != null)
            couponService.release(order.getUserId(), order.getCouponId(), order.getId());
        order.setStatus("EXPIRED");
    }

    @Override
    public List<OrderViewVO> list(Long userId, String status) {
        String normalizedStatus = status == null ? null : status.trim();
        return ticketOrderMapper.selectList(Wrappers.<TicketOrder>lambdaQuery()
                        .eq(TicketOrder::getUserId, userId)
                        .eq(normalizedStatus != null && !normalizedStatus.isEmpty(),
                                TicketOrder::getStatus, normalizedStatus)
                        .orderByDesc(TicketOrder::getCreatedAt))
                .stream().map(order -> toView(order, true)).toList();
    }

    @Override
    public OrderViewVO detail(Long userId, Long orderId) {
        TicketOrder order = ticketOrderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(SystemResultCode.NOT_FOUND);
        }
        return toView(order, true);
    }

    private OrderViewVO toView(TicketOrder order, boolean includeTickets) {
        TicketType type = ticketTypeMapper.selectById(order.getTicketTypeId());
        ActivitySession session = type == null ? null : activitySessionMapper.selectById(type.getSessionId());
        Activity activity = session == null ? null : activityMapper.selectById(session.getActivityId());

        OrderViewVO view = new OrderViewVO();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setUserId(order.getUserId());
        view.setRealName(order.getRealName());
        view.setIdCardMasked(order.getIdCardMasked());
        view.setTicketTypeId(order.getTicketTypeId());
        view.setTicketTypeName(type == null ? null : type.getName());
        view.setQuantity(order.getQuantity());
        view.setAmount(order.getAmount());
        view.setDiscountAmount(order.getDiscountAmount());
        view.setCouponId(order.getCouponId());
        if (order.getCouponId() != null) {
            Coupon coupon = couponMapper.selectById(order.getCouponId());
            view.setCouponName(coupon == null ? null : coupon.getName());
        }
        view.setPayAmount(order.getPayAmount());
        view.setStatus(order.getStatus());
        view.setExpiredAt(order.getExpiredAt());
        view.setPaidAt(order.getPaidAt());
        view.setCreatedAt(order.getCreatedAt());
        if (session != null) {
            view.setSessionId(session.getId());
            view.setSessionStartTime(session.getStartTime());
            view.setSessionEndTime(session.getEndTime());
        }
        if (activity != null) {
            view.setActivityId(activity.getId());
            view.setActivityTitle(activity.getTitle());
            view.setVenueName(activity.getVenueName());
            view.setCoverFileUrl(fileStorageService.getUrl(activity.getCoverFileId()));
        }
        if (includeTickets) {
            view.setTickets(ticketMapper.selectList(Wrappers.<Ticket>lambdaQuery()
                            .eq(Ticket::getOrderId, order.getId())
                            .orderByAsc(Ticket::getId))
                    .stream().map(ticket -> new TicketViewVO(ticket.getId(),
                            ticket.getVerificationCode(), ticket.getStatus().name())).toList());
        }
        refundService.enrich(view, order, includeTickets);
        view.setServerTime(clock.instant());
        return view;
    }

    private void validateSaleable(TicketType type, int quantity) {
        if (!Boolean.TRUE.equals(type.getEnabled())) {
            throw new BusinessException(SystemResultCode.TICKET_TYPE_NOT_ON_SALE);
        }
        if (quantity < 1) {
            throw new BusinessException(SystemResultCode.BAD_REQUEST);
        }
        if (type.getStock() == null || type.getStock() < quantity) {
            throw new BusinessException(SystemResultCode.TICKET_STOCK_NOT_ENOUGH);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        ActivitySession session = activitySessionMapper.selectById(type.getSessionId());
        if (session == null || session.getStatus() != SessionStatus.ON_SALE
                || session.getEndTime() == null || !session.getEndTime().isAfter(now)) {
            throw new BusinessException(SystemResultCode.SESSION_NOT_ON_SALE);
        }
        Activity activity = activityMapper.selectById(session.getActivityId());
        if (activity == null || activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(SystemResultCode.ACTIVITY_NOT_PUBLISHED);
        }
        if (type.getSaleStartTime() == null || type.getSaleEndTime() == null
                || now.isBefore(type.getSaleStartTime()) || now.isAfter(type.getSaleEndTime())) {
            throw new BusinessException(SystemResultCode.TICKET_TYPE_NOT_ON_SALE);
        }
    }


    private String activityCategory(TicketType type) {
        ActivitySession session = activitySessionMapper.selectById(type.getSessionId());
        Activity activity = session == null ? null : activityMapper.selectById(session.getActivityId());
        return activity == null || activity.getCategory() == null ? null : activity.getCategory().name();
    }

    private String normalizeIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() < 8 || value.length() > 128)
            throw new IllegalArgumentException(OrderMessageConstant.IDEMPOTENCY_KEY_INVALID);
        return value;
    }

    private boolean tryAcquireCreateLock(Long userId, String key) {
        if (redis == null) return true;
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("order:create:{" + userId + "}:" + sha256(key), "1", java.time.Duration.ofSeconds(20)));
        } catch (DataAccessException ex) {
            return true;
        }
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder(64);
            for (byte x : bytes) b.append(String.format("%02x", x));
            return b.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String generateBusinessNo() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
