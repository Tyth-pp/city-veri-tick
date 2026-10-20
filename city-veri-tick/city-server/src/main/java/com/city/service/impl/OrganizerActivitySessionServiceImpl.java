package com.city.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.result.SystemResultCode;
import com.city.dto.CreateActivitySessionDTO;
import com.city.dto.UpdateActivitySessionDTO;
import com.city.entity.Activity;
import com.city.entity.ActivitySession;
import com.city.entity.TicketType;
import com.city.mapper.TicketTypeMapper;
import com.city.enums.ActivityStatus;
import com.city.enums.SessionStatus;
import com.city.exception.BusinessException;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivitySessionMapper;
import com.city.service.OrganizerAccessService;
import com.city.service.OrganizerActivitySessionService;
import com.city.vo.ActivitySessionViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganizerActivitySessionServiceImpl implements OrganizerActivitySessionService {
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private ActivitySessionMapper activitySessionMapper;
    @Autowired
    private OrganizerAccessService organizerAccessService;
    @Autowired
    private TicketTypeMapper ticketTypeMapper;

    @Override
    @Transactional
    public ActivitySessionViewVO create(Long userId, Long activityId, CreateActivitySessionDTO dto) {
        requireEditableActivity(userId, activityId);
        validateTime(dto.getStartTime(), dto.getEndTime());
        validateCapacity(dto.getCapacity());
        validateSaleTime(dto.getSaleStartTime(), dto.getSaleEndTime(), dto.getStartTime());
        ActivitySession session = new ActivitySession();
        session.setActivityId(activityId);
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());
        session.setSaleStartTime(dto.getSaleStartTime());
        session.setSaleEndTime(dto.getSaleEndTime());
        session.setCapacity(dto.getCapacity());
        session.setSoldCount(0);
        session.setStatus(SessionStatus.DRAFT);
        activitySessionMapper.insert(session);
        return ActivitySessionViewVO.from(session);
    }

    @Override
    public List<ActivitySessionViewVO> list(Long userId, Long activityId) {
        requireOwnedActivity(userId, activityId);
        return activitySessionMapper.selectList(Wrappers.<ActivitySession>lambdaQuery()
                        .eq(ActivitySession::getActivityId, activityId)
                        .orderByAsc(ActivitySession::getStartTime))
                .stream().map(ActivitySessionViewVO::from).toList();
    }

    @Override
    @Transactional
    public ActivitySessionViewVO update(Long userId, Long sessionId, UpdateActivitySessionDTO dto) {
        ActivitySession session = requireOwnedSession(userId, sessionId);
        requireEditableActivity(userId, session.getActivityId());
        validateTime(dto.getStartTime(), dto.getEndTime());
        validateCapacity(dto.getCapacity());
        LocalDateTime saleStart = dto.getSaleStartTime() == null && dto.getSaleEndTime() == null
                ? session.getSaleStartTime() : dto.getSaleStartTime();
        LocalDateTime saleEnd = dto.getSaleStartTime() == null && dto.getSaleEndTime() == null
                ? session.getSaleEndTime() : dto.getSaleEndTime();
        validateSaleTime(saleStart, saleEnd, dto.getStartTime());
        if (session.getStatus() != SessionStatus.DRAFT || (session.getSoldCount() != null && session.getSoldCount() > 0)) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        List<TicketType> types = ticketTypeMapper.selectList(Wrappers.<TicketType>lambdaQuery()
                .eq(TicketType::getSessionId, sessionId));
        int allocated = types.stream().mapToInt(type -> (type.getStock() == null ? 0 : type.getStock())
                + (type.getLockedStock() == null ? 0 : type.getLockedStock())).sum();
        if (allocated > dto.getCapacity()) throw new BusinessException(SystemResultCode.SESSION_CAPACITY_EXCEEDED);
        // Legacy sessions retain their per-ticket windows until a shared window is explicitly saved.
        if (saleStart == null) {
            for (TicketType type : types) {
                if (type.getSaleEndTime() != null && type.getSaleEndTime().isAfter(dto.getStartTime()))
                    throw new BusinessException(SystemResultCode.SALE_TIME_AFTER_SESSION);
            }
        } else {
            for (TicketType type : types) {
                type.setSaleStartTime(saleStart);
                type.setSaleEndTime(saleEnd);
                ticketTypeMapper.updateById(type);
            }
        }
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());
        session.setSaleStartTime(saleStart);
        session.setSaleEndTime(saleEnd);
        session.setCapacity(dto.getCapacity());
        activitySessionMapper.updateById(session);
        return ActivitySessionViewVO.from(session);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long sessionId) {
        ActivitySession session = requireOwnedSession(userId, sessionId);
        requireEditableActivity(userId, session.getActivityId());
        if (session.getSoldCount() > 0 || session.getStatus() != SessionStatus.DRAFT) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        activitySessionMapper.deleteById(sessionId);
    }

    private Activity requireEditableActivity(Long userId, Long activityId) {
        Activity activity = requireOwnedActivity(userId, activityId);
        if (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.REJECTED) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        return activity;
    }

    private Activity requireOwnedActivity(Long userId, Long activityId) {
        Long organizerId = organizerAccessService.requireOrganizerId(userId);
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        if (!organizerId.equals(activity.getOrganizerId())) throw new BusinessException(SystemResultCode.FORBIDDEN);
        return activity;
    }

    private ActivitySession requireOwnedSession(Long userId, Long sessionId) {
        ActivitySession session = activitySessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        requireOwnedActivity(userId, session.getActivityId());
        return session;
    }

    private void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) throw new BusinessException(SystemResultCode.SESSION_TIME_REQUIRED);
        if (!endTime.isAfter(startTime)) throw new BusinessException(SystemResultCode.SESSION_TIME_INVALID);
    }

    private void validateCapacity(Integer capacity) {
        if (capacity == null || capacity < 1) throw new BusinessException(SystemResultCode.SESSION_CAPACITY_INVALID);
    }

    private void validateSaleTime(LocalDateTime start, LocalDateTime end, LocalDateTime sessionStart) {
        if (start == null && end == null) return;
        if (start == null || end == null) throw new BusinessException(SystemResultCode.SALE_TIME_REQUIRED);
        if (!end.isAfter(start)) throw new BusinessException(SystemResultCode.SALE_TIME_INVALID);
        if (end.isAfter(sessionStart)) throw new BusinessException(SystemResultCode.SALE_TIME_AFTER_SESSION);
    }
}
