package com.city.service.impl;

import com.city.service.RefundService;
import com.city.service.support.ManagementOrderSupport;

import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.entity.*;
import com.city.enums.TicketStatus;
import com.city.exception.BusinessException;
import com.city.result.SystemResultCode;
import com.city.mapper.*;
import com.city.service.PlatformOrderService;
import com.city.service.FileStorageService;
import com.city.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlatformOrderServiceImpl implements PlatformOrderService {
    @Autowired
    private RefundService refundService;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private ActivitySessionMapper sessionMapper;
    @Autowired
    private TicketTypeMapper ticketTypeMapper;
    @Autowired
    private TicketOrderMapper orderMapper;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private CheckInRecordMapper checkInRecordMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrganizerMapper organizerMapper;
    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public List<OrderViewVO> list(String status) {
        String normalized = status == null ? null : status.trim();
        return orderMapper.selectList(Wrappers.<TicketOrder>lambdaQuery()
                        .eq(normalized != null && !normalized.isEmpty(), TicketOrder::getStatus, normalized)
                        .orderByDesc(TicketOrder::getCreatedAt))
                .stream().map(order -> toView(order, false)).toList();
    }

    @Override
    public OrderViewVO detail(Long orderId) {
        TicketOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        return toView(order, true);
    }

    @Override
    public OrganizerVerificationSummaryVO verificationSummary() {
        List<Ticket> tickets = ticketMapper.selectList(Wrappers.emptyWrapper());
        OrganizerVerificationSummaryVO view = new OrganizerVerificationSummaryVO();
        view.setTotalTickets((long) tickets.size());
        view.setUsedTickets(tickets.stream().filter(t -> t.getStatus() == TicketStatus.CHECKED_IN).count());
        view.setUnusedTickets(tickets.stream().filter(t -> t.getStatus() == TicketStatus.UNUSED).count());
        view.setCancelledTickets(tickets.stream().filter(t -> t.getStatus() == TicketStatus.REFUNDED).count());
        view.setTotalCheckInRecords((long) checkInRecordMapper.selectCount(Wrappers.emptyWrapper()));
        return view;
    }

    @Override
    public List<OrganizerVerificationRecordVO> verificationRecords(Boolean checkedIn) {
        Map<Long, TicketType> types = ticketTypeMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(TicketType::getId, item -> item));
        Map<Long, ActivitySession> sessions = new HashMap<>();
        Map<Long, Activity> activities = new HashMap<>();
        List<OrganizerVerificationRecordVO> records = new ArrayList<>();
        for (Ticket ticket : ticketMapper.selectList(Wrappers.<Ticket>lambdaQuery().orderByDesc(Ticket::getUpdatedAt))) {
            boolean isChecked = ticket.getStatus() == TicketStatus.CHECKED_IN;
            if (checkedIn != null && checkedIn != isChecked) continue;
            TicketType type = types.get(ticket.getTicketTypeId());
            if (type == null) continue;
            ActivitySession session = sessions.computeIfAbsent(type.getSessionId(), sessionMapper::selectById);
            if (session == null) continue;
            Activity activity = activities.computeIfAbsent(session.getActivityId(), activityMapper::selectById);
            if (activity == null) continue;
            CheckInRecord record = checkInRecordMapper.selectOne(Wrappers.<CheckInRecord>lambdaQuery()
                    .eq(CheckInRecord::getTicketId, ticket.getId()).orderByDesc(CheckInRecord::getCheckedInAt).last("LIMIT 1"));
            TicketOrder order = orderMapper.selectById(ticket.getOrderId());
            OrganizerVerificationRecordVO view = new OrganizerVerificationRecordVO();
            view.setTicketId(ticket.getId());
            view.setTicketNo(ticket.getTicketNo());
            view.setVerificationCode(ticket.getVerificationCode());
            view.setTicketStatus(ticket.getStatus().name());
            view.setOrderId(ticket.getOrderId());
            view.setOrderNo(order == null ? null : order.getOrderNo());
            view.setOrderStatus(order == null ? null : order.getStatus());
            view.setActivityId(activity.getId());
            view.setActivityTitle(activity.getTitle());
            view.setSessionId(session.getId());
            view.setSessionStartTime(session.getStartTime());
            view.setSessionEndTime(session.getEndTime());
            view.setTicketTypeName(type.getName());
            view.setCoverFileUrl(fileStorageService.getUrl(activity.getCoverFileId()));
            if (record != null) {
                view.setVerifierId(record.getVerifierId());
                User operator = userMapper.selectById(record.getVerifierId());
                if (operator != null) {
                    view.setVerifierName(operator.getStaffName());
                    view.setVerifierUsername(operator.getUsername());
                }
                view.setCheckedInAt(record.getCheckedInAt());
            }
            records.add(view);
        }
        return records;
    }

    private OrderViewVO toView(TicketOrder order, boolean includeTickets) {
        TicketType type = ticketTypeMapper.selectById(order.getTicketTypeId());
        ActivitySession session = type == null ? null : sessionMapper.selectById(type.getSessionId());
        Activity activity = session == null ? null : activityMapper.selectById(session.getActivityId());
        OrderViewVO view = new OrderViewVO();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setUserId(order.getUserId());
        view.setTicketTypeId(order.getTicketTypeId());
        view.setTicketTypeName(type == null ? null : type.getName());
        view.setQuantity(order.getQuantity());
        view.setAmount(order.getAmount());
        view.setDiscountAmount(order.getDiscountAmount());
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
            view.setCoverFileUrl(fileStorageService.getUrl(activity.getCoverFileId()));
        }
        ManagementOrderSupport.identity(view, order, activity, organizerMapper);
        if (includeTickets)
            view.setTickets(ticketMapper.selectList(Wrappers.<Ticket>lambdaQuery().eq(Ticket::getOrderId, order.getId()).orderByAsc(Ticket::getId))
                    .stream().map(t -> ManagementOrderSupport.ticket(t, checkInRecordMapper, userMapper)).toList());
        refundService.enrich(view, order, includeTickets);
        return view;
    }
}
