package com.city.service.impl;

import com.city.service.RefundService;
import com.city.service.support.ManagementOrderSupport;

import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.result.SystemResultCode;
import com.city.entity.*;
import com.city.enums.TicketStatus;
import com.city.exception.BusinessException;
import com.city.mapper.*;
import com.city.service.OrganizerAccessService;
import com.city.service.OrganizerOrderService;
import com.city.service.FileStorageService;
import com.city.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrganizerOrderServiceImpl implements OrganizerOrderService {
    @Autowired
    private RefundService refundService;
    @Autowired
    private OrganizerAccessService organizerAccessService;
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
    public List<OrderViewVO> list(Long userId, String status) {
        Set<Long> ticketTypeIds = organizerTicketTypeIds(userId);
        if (ticketTypeIds.isEmpty()) return List.of();
        String normalized = status == null ? null : status.trim();
        return orderMapper.selectList(Wrappers.<TicketOrder>lambdaQuery()
                        .in(TicketOrder::getTicketTypeId, ticketTypeIds)
                        .eq(normalized != null && !normalized.isEmpty(), TicketOrder::getStatus, normalized)
                        .orderByDesc(TicketOrder::getCreatedAt))
                .stream().map(order -> toView(order, false)).toList();
    }

    @Override
    public OrderViewVO detail(Long userId, Long orderId) {
        TicketOrder order = orderMapper.selectById(orderId);
        if (order == null || !organizerTicketTypeIds(userId).contains(order.getTicketTypeId())) {
            throw new BusinessException(SystemResultCode.NOT_FOUND);
        }
        return toView(order, true);
    }

    @Override
    public OrganizerVerificationSummaryVO verificationSummary(Long userId) {
        List<Ticket> tickets = organizerTickets(userId);
        OrganizerVerificationSummaryVO view = new OrganizerVerificationSummaryVO();
        view.setTotalTickets((long) tickets.size());
        view.setUsedTickets(tickets.stream().filter(t -> t.getStatus() == TicketStatus.CHECKED_IN).count());
        view.setUnusedTickets(tickets.stream().filter(t -> t.getStatus() == TicketStatus.UNUSED).count());
        view.setCancelledTickets(tickets.stream().filter(t -> t.getStatus() == TicketStatus.REFUNDED).count());
        view.setTotalCheckInRecords(tickets.isEmpty() ? 0L : checkInRecordMapper.selectCount(Wrappers.<CheckInRecord>lambdaQuery()
                .in(CheckInRecord::getTicketId, tickets.stream().map(Ticket::getId).toList())));
        return view;
    }

    @Override
    public List<OrganizerVerificationRecordVO> verificationRecords(Long userId, Boolean checkedIn) {
        Map<Long, TicketType> types = organizerTicketTypes(userId);
        if (types.isEmpty()) return List.of();
        Map<Long, ActivitySession> sessions = new HashMap<>();
        Map<Long, Activity> activities = new HashMap<>();
        List<OrganizerVerificationRecordVO> records = new ArrayList<>();
        for (Ticket ticket : organizerTickets(userId)) {
            boolean isChecked = ticket.getStatus() == TicketStatus.CHECKED_IN;
            if (checkedIn != null && checkedIn != isChecked) continue;
            TicketType type = types.get(ticket.getTicketTypeId());
            ActivitySession session = sessions.computeIfAbsent(type.getSessionId(), sessionMapper::selectById);
            Activity activity = activities.computeIfAbsent(session.getActivityId(), activityMapper::selectById);
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
            view.setCoverFileUrl(fileStorageService.getUrl(activity.getCoverFileId()));
            view.setTicketTypeName(type.getName());
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

    private Set<Long> organizerTicketTypeIds(Long userId) {
        return organizerTicketTypes(userId).keySet();
    }

    private Map<Long, TicketType> organizerTicketTypes(Long userId) {
        Long organizerId = organizerAccessService.requireOrganizerId(userId);
        Set<Long> activityIds = activityMapper.selectList(Wrappers.<Activity>lambdaQuery().eq(Activity::getOrganizerId, organizerId))
                .stream().map(Activity::getId).collect(Collectors.toSet());
        if (activityIds.isEmpty()) return Map.of();
        Set<Long> sessionIds = sessionMapper.selectList(Wrappers.<ActivitySession>lambdaQuery().in(ActivitySession::getActivityId, activityIds))
                .stream().map(ActivitySession::getId).collect(Collectors.toSet());
        if (sessionIds.isEmpty()) return Map.of();
        return ticketTypeMapper.selectList(Wrappers.<TicketType>lambdaQuery().in(TicketType::getSessionId, sessionIds))
                .stream().collect(Collectors.toMap(TicketType::getId, t -> t));
    }

    private List<Ticket> organizerTickets(Long userId) {
        Set<Long> ids = organizerTicketTypeIds(userId);
        return ids.isEmpty() ? List.of() : ticketMapper.selectList(Wrappers.<Ticket>lambdaQuery().in(Ticket::getTicketTypeId, ids));
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
