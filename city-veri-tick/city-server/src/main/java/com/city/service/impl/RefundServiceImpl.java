package com.city.service.impl;
import com.city.constant.ManagementMessageConstant;
import com.city.constant.RefundMessageConstant;
import com.city.service.CouponService;
import com.city.service.ManagementMessageService;
import com.city.service.RefundService;
import com.city.exception.RefundException;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.entity.*;
import com.city.enums.TicketStatus;
import com.city.mapper.*;
import com.city.service.OrganizerAccessService;
import com.city.vo.OrderViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class RefundServiceImpl implements RefundService {
    @Autowired
    private TicketOrderMapper orders;
    @Autowired
    private TicketMapper tickets;
    @Autowired
    private TicketTypeMapper types;
    @Autowired
    private ActivitySessionMapper sessions;
    @Autowired
    private ActivityMapper activities;
    @Autowired
    private RefundRequestMapper refunds;
    @Autowired
    private OrganizerAccessService access;
    @Autowired
    private UserMapper users;
    @Autowired
    private Clock clock;
    @Autowired
    private ManagementMessageService messages;
    @Autowired(required = false) private CouponService couponService;

    private static RefundException error(String message) {
        return new RefundException(message);
    }

    private static String reason(String value, boolean required, String label) {
        String text = value == null ? "" : value.trim();
        if (required && text.isEmpty()) throw error(RefundMessageConstant.REQUIRED_TEMPLATE.formatted(label));
        if (text.length() > 500) throw error(RefundMessageConstant.TOO_LONG_TEMPLATE.formatted(label));
        return text;
    }

    @Transactional
    @Override
    public RefundRequest apply(Long userId, Long orderId, String explanation) {
        String text = reason(explanation, true, RefundMessageConstant.APPLICATION_REASON_LABEL);
        // All refund and check-in writers acquire the order lock before ticket locks.
        TicketOrder order = orders.selectByIdForUpdate(orderId);
        if (order == null || !userId.equals(order.getUserId())) throw error(RefundMessageConstant.ORDER_NOT_OWNED_BY_USER);
        List<Ticket> issued = tickets.selectForRefund(orderId);
        String blocked = blockedReason(order, issued);
        if (blocked != null) throw error(blocked);
        if (orders.changeRefundStatus(orderId, "PAID", "REFUNDING") != 1) throw error(RefundMessageConstant.ORDER_CHANGED_RETRY);
        RefundRequest request = new RefundRequest();
        request.setOrderId(orderId);
        request.setUserId(userId);
        request.setAmount(order.getPayAmount());
        request.setStatus("PENDING");
        request.setReason(text);
        request.setPaymentMode("SIMULATED");
        request.setRequestedAt(LocalDateTime.now(clock));
        if (refunds.insert(request) != 1) throw error(RefundMessageConstant.APPLICATION_SAVE_FAILED);
        TicketType type = types.selectById(order.getTicketTypeId());
        ActivitySession session = sessions.selectById(type.getSessionId());
        Activity activity = activities.selectById(session.getActivityId());
        if (activity == null) throw error(RefundMessageConstant.ACTIVITY_INCOMPLETE);
        messages.publish("ORGANIZER_ADMIN", activity.getOrganizerId(), "REFUND_REQUESTED", ManagementMessageConstant.REFUND_REQUESTED,
                ManagementMessageConstant.REFUND_CONTENT_TEMPLATE.formatted(activity.getTitle(), request.getAmount().toPlainString()),
                "/management/organizer/orders/" + orderId);
        return request;
    }

    @Transactional
    @Override
    public RefundRequest review(Long actor, Long orderId, Long refundId, boolean approved, String explanation) {
        String text = reason(explanation, !approved, approved ? RefundMessageConstant.REVIEW_EXPLANATION_LABEL : RefundMessageConstant.REJECTION_REASON_LABEL);
        TicketOrder order = orders.selectByIdForUpdate(orderId);
        requireOrganizer(actor, order);
        RefundRequest request = refunds.selectById(refundId);
        if (request == null || !orderId.equals(request.getOrderId())) throw error(RefundMessageConstant.REQUEST_NOT_FOUND);
        if (!"REFUNDING".equals(order.getStatus()) || !"PENDING".equals(request.getStatus()))
            throw error(RefundMessageConstant.REQUEST_ALREADY_PROCESSED);
        List<Ticket> issued = tickets.selectForRefund(orderId);
        if (issued.size() != order.getQuantity() || issued.stream().anyMatch(t -> t.getStatus() != TicketStatus.UNUSED))
            throw error(RefundMessageConstant.TICKET_STATE_INVALID);
        if (request.getAmount() == null || order.getPayAmount() == null || request.getAmount().compareTo(order.getPayAmount()) != 0)
            throw error(RefundMessageConstant.AMOUNT_MISMATCH);
        if (approved) {
            if (tickets.refundUnused(orderId) != order.getQuantity()) throw error(RefundMessageConstant.TICKETS_CHANGED);
            if (types.restoreRefundedStock(order.getTicketTypeId(), order.getQuantity()) != 1)
                throw error(RefundMessageConstant.STOCK_RESTORE_FAILED);
            if (order.getCouponId() != null && couponService != null) couponService.restoreAfterRefund(order.getUserId(), order.getCouponId(), order.getId());
            request.setRefundNo("SIM" + UUID.randomUUID().toString().replace("-", ""));
        }
        String next = approved ? "REFUNDED" : "PAID";
        if (orders.changeRefundStatus(orderId, "REFUNDING", next) != 1) throw error(RefundMessageConstant.ORDER_CHANGED_INCOMPLETE);
        User reviewer = users.selectById(actor);
        if (reviewer == null) throw error(RefundMessageConstant.REVIEWER_NOT_FOUND);
        request.setStatus(approved ? "REFUNDED" : "REJECTED");
        request.setReviewReason(text.isEmpty() ? null : text);
        request.setReviewerId(actor);
        request.setReviewerName(reviewer.getStaffName());
        request.setReviewerUsername(reviewer.getUsername());
        request.setProcessedAt(LocalDateTime.now(clock));
        if (refunds.updateById(request) != 1) throw error(RefundMessageConstant.REVIEW_SAVE_FAILED);
        return request;
    }

    private void requireOrganizer(Long actor, TicketOrder order) {
        Long organizer = access.requireOrganizerId(actor);
        TicketType type = order == null ? null : types.selectById(order.getTicketTypeId());
        ActivitySession session = type == null ? null : sessions.selectById(type.getSessionId());
        Activity activity = session == null ? null : activities.selectById(session.getActivityId());
        if (activity == null || !organizer.equals(activity.getOrganizerId()))
            throw error(RefundMessageConstant.ORDER_NOT_OWNED_BY_ORGANIZER);
    }

    @Override
    public void enrich(OrderViewVO view, TicketOrder order, boolean detail) {
        List<RefundRequest> history = refunds.selectList(Wrappers.<RefundRequest>lambdaQuery().eq(RefundRequest::getOrderId, order.getId()).orderByDesc(RefundRequest::getRequestedAt).orderByDesc(RefundRequest::getId).last(detail ? "" : "LIMIT 1"));
        view.setLatestRefund(history.isEmpty() ? null : history.get(0));
        if (detail) {
            view.setRefundHistory(history);
            view.setRefundRule(RULE);
            List<Ticket> issued = tickets.selectList(Wrappers.<Ticket>lambdaQuery().eq(Ticket::getOrderId, order.getId()));
            String blocked = blockedReason(order, issued);
            view.setRefundEligible(blocked == null);
            view.setRefundBlockedReason(blocked);
        }
    }

    private String blockedReason(TicketOrder order, List<Ticket> issued) {
        if ("REFUNDING".equals(order.getStatus())) return RefundMessageConstant.REQUEST_IN_PROGRESS;
        if ("REFUNDED".equals(order.getStatus())) return RefundMessageConstant.ORDER_ALREADY_REFUNDED;
        if (!"PAID".equals(order.getStatus())) return RefundMessageConstant.ORDER_NOT_PAID;
        if (order.getPayAmount() == null || order.getPayAmount().signum() < 0) return RefundMessageConstant.PAYMENT_AMOUNT_INVALID;
        if (order.getQuantity() == null || issued.size() != order.getQuantity() || issued.isEmpty())
            return RefundMessageConstant.TICKETS_INCOMPLETE;
        if (issued.stream().anyMatch(t -> t.getStatus() == TicketStatus.CHECKED_IN))
            return RefundMessageConstant.TICKET_ALREADY_USED;
        if (issued.stream().anyMatch(t -> t.getStatus() != TicketStatus.UNUSED)) return RefundMessageConstant.TICKET_STATUS_UNSUPPORTED;
        TicketType type = types.selectById(order.getTicketTypeId());
        ActivitySession session = type == null ? null : sessions.selectById(type.getSessionId());
        if (session == null || session.getStartTime() == null) return RefundMessageConstant.SESSION_TIME_INCOMPLETE;
        if (LocalDateTime.now(clock).plusHours(48).isAfter(session.getStartTime()))
            return RefundMessageConstant.TOO_CLOSE_TO_SESSION;
        return null;
    }
}
