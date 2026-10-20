package com.city.service.impl;

import com.city.constant.TicketMessageConstant;
import com.city.exception.RefundException;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.result.SystemResultCode;
import com.city.entity.*;
import com.city.enums.TicketStatus;
import com.city.exception.BusinessException;
import com.city.mapper.*;
import com.city.service.CheckInService;
import com.city.vo.VerifyTicketViewVO;
import com.city.vo.VerifierStatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CheckInServiceImpl implements CheckInService {
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private CheckInRecordMapper checkInRecordMapper;
    @Autowired
    private TicketTypeMapper ticketTypeMapper;
    @Autowired
    private ActivitySessionMapper sessionMapper;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private ActivityVerifierMapper verifierMapper;
    @Autowired
    private TicketOrderMapper orderMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public VerifyTicketViewVO checkIn(Long verifierId, String code, String idCardNo) {
        Ticket ticket = findTicket(code);
        VerifyTicketViewVO view = buildView(ticket);
        verifyPermission(verifierId, view.getActivityId());
        verifyIdentity(ticket, idCardNo);
        TicketOrder lockedOrder = orderMapper.selectByIdForUpdate(ticket.getOrderId());
        if (lockedOrder != null && "REFUNDING".equals(lockedOrder.getStatus()))
            throw new RefundException(TicketMessageConstant.ORDER_REFUNDING);
        if (lockedOrder == null || !"PAID".equals(lockedOrder.getStatus()))
            throw new RefundException(TicketMessageConstant.ORDER_NOT_PAYABLE_FOR_CHECK_IN);
        Long ticketId = ticket.getId();
        ticket = ticketMapper.selectForRefund(lockedOrder.getId()).stream().filter(t -> ticketId.equals(t.getId())).findFirst().orElseThrow(() -> new RefundException(TicketMessageConstant.RECORD_NOT_FOUND));
        if (ticket.getStatus() != TicketStatus.UNUSED)
            throw new BusinessException(SystemResultCode.TICKET_ALREADY_CHECKED_IN);
        int changed = ticketMapper.update(null, Wrappers.<Ticket>lambdaUpdate().eq(Ticket::getId, ticket.getId()).eq(Ticket::getStatus, TicketStatus.UNUSED).set(Ticket::getStatus, TicketStatus.CHECKED_IN));
        if (changed != 1) throw new BusinessException(SystemResultCode.TICKET_ALREADY_CHECKED_IN);
        CheckInRecord record = new CheckInRecord();
        record.setTicketId(ticket.getId());
        record.setVerifierId(verifierId);
        record.setCheckedInAt(LocalDateTime.now());
        checkInRecordMapper.insert(record);
        view.setTicketStatus(TicketStatus.CHECKED_IN.name());
        view.setCheckedInAt(record.getCheckedInAt());
        view.setMessage(TicketMessageConstant.CHECK_IN_SUCCESS);
        return view;
    }

    @Override
    public VerifyTicketViewVO detail(Long verifierId, String code) {
        Ticket ticket = findTicket(code);
        VerifyTicketViewVO view = buildView(ticket);
        verifyPermission(verifierId, view.getActivityId());
        CheckInRecord record = checkInRecordMapper.selectOne(Wrappers.<CheckInRecord>lambdaQuery().eq(CheckInRecord::getTicketId, ticket.getId()).last("LIMIT 1"));
        if (record != null) {
            view.setCheckedInAt(record.getCheckedInAt());
            view.setMessage(TicketMessageConstant.ALREADY_CHECKED_IN);
        } else
            view.setMessage("REFUNDING".equals(view.getOrderStatus()) ? TicketMessageConstant.CHECK_IN_REFUNDING : "REFUNDED".equals(view.getOrderStatus()) || ticket.getStatus() == TicketStatus.REFUNDED ? TicketMessageConstant.CHECK_IN_REFUNDED : "PAID".equals(view.getOrderStatus()) ? TicketMessageConstant.CHECK_IN_AVAILABLE : TicketMessageConstant.CHECK_IN_ORDER_STATUS_INVALID);
        return view;
    }

    @Override
    public List<VerifyTicketViewVO> records(Long verifierId) {
        return checkInRecordMapper.selectList(Wrappers.<CheckInRecord>lambdaQuery().eq(CheckInRecord::getVerifierId, verifierId).orderByDesc(CheckInRecord::getCheckedInAt).last("LIMIT 50")).stream().map(record -> {
            VerifyTicketViewVO view = buildView(ticketMapper.selectById(record.getTicketId()));
            view.setCheckedInAt(record.getCheckedInAt());
            return view;
        }).toList();
    }

    @Override
    public VerifierStatisticsVO statistics(Long verifierId) {
        List<CheckInRecord> records = checkInRecordMapper.selectList(Wrappers.<CheckInRecord>lambdaQuery().eq(CheckInRecord::getVerifierId, verifierId));
        LocalDate today = LocalDate.now();
        VerifierStatisticsVO view = new VerifierStatisticsVO();
        view.setTotalCheckedIn((long) records.size());
        view.setTodayCheckedIn(records.stream().filter(r -> r.getCheckedInAt() != null && r.getCheckedInAt().toLocalDate().equals(today)).count());
        return view;
    }

    private Ticket findTicket(String code) {
        Ticket ticket = ticketMapper.selectOne(Wrappers.<Ticket>lambdaQuery().eq(Ticket::getVerificationCode, code));
        if (ticket == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        return ticket;
    }

    private void verifyIdentity(Ticket ticket, String idCardNo) {
        TicketOrder order = orderMapper.selectById(ticket.getOrderId());
        if (order == null || order.getIdCardHash() == null || idCardNo == null
                || !passwordEncoder.matches(idCardNo.trim().toUpperCase(Locale.ROOT), order.getIdCardHash())) {
            throw new BusinessException(SystemResultCode.ID_CARD_MISMATCH);
        }
    }

    private void verifyPermission(Long verifierId, Long activityId) {
        LocalDateTime now = LocalDateTime.now();
        Long count = verifierMapper.selectCount(Wrappers.<ActivityVerifier>lambdaQuery().eq(ActivityVerifier::getUserId, verifierId).eq(ActivityVerifier::getActivityId, activityId).eq(ActivityVerifier::getEnabled, true).and(w -> w.isNull(ActivityVerifier::getValidFrom).or().le(ActivityVerifier::getValidFrom, now)).and(w -> w.isNull(ActivityVerifier::getValidUntil).or().ge(ActivityVerifier::getValidUntil, now)));
        if (count == 0) throw new BusinessException(SystemResultCode.FORBIDDEN);
    }

    private VerifyTicketViewVO buildView(Ticket ticket) {
        TicketType type = ticketTypeMapper.selectById(ticket.getTicketTypeId());
        ActivitySession session = type == null ? null : sessionMapper.selectById(type.getSessionId());
        Activity activity = session == null ? null : activityMapper.selectById(session.getActivityId());
        VerifyTicketViewVO view = new VerifyTicketViewVO();
        view.setTicketId(ticket.getId());
        view.setTicketNo(ticket.getTicketNo());
        view.setVerificationCode(ticket.getVerificationCode());
        view.setTicketStatus(ticket.getStatus().name());
        view.setTicketTypeName(type == null ? null : type.getName());
        if (session != null) {
            view.setSessionId(session.getId());
            view.setSessionStartTime(session.getStartTime());
            view.setSessionEndTime(session.getEndTime());
        }
        if (activity != null) {
            view.setActivityId(activity.getId());
            view.setActivityTitle(activity.getTitle());
        }
        TicketOrder order = orderMapper.selectById(ticket.getOrderId());
        if (order != null) {
            view.setOrderStatus(order.getStatus());
            view.setRealName(order.getRealName());
            view.setIdCardMasked(order.getIdCardMasked());
        }
        return view;
    }
}
