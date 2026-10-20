package com.city.service.impl;

import com.city.cache.ActivityListCache;
import com.city.cache.PublicActivityCache;

import java.util.ArrayList;
import java.util.Objects;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.result.SystemResultCode;
import com.city.entity.Activity;
import com.city.entity.ActivitySession;
import com.city.entity.TicketType;
import com.city.enums.ActivityStatus;
import com.city.enums.SessionStatus;
import com.city.exception.BusinessException;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivitySessionMapper;
import com.city.mapper.TicketTypeMapper;
import com.city.service.UserActivityService;
import com.city.service.FileStorageService;
import com.city.vo.UserActivityDetailVO;
import com.city.vo.UserActivitySessionVO;
import com.city.vo.UserActivitySummaryVO;
import com.city.vo.UserActivityPreviewVO;
import com.city.vo.UserTicketTypeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class UserActivityServiceImpl implements UserActivityService {
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private PublicActivityCache publicActivityCache;
    @Autowired
    private ActivityListCache activityListCache;
    @Autowired
    private ActivitySessionMapper activitySessionMapper;
    @Autowired
    private TicketTypeMapper ticketTypeMapper;
    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public List<UserActivitySummaryVO> list(String keyword, String category) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        String normalizedCategory = category == null ? null : category.trim();
        List<Activity> activities = activityListCache.getPublished(normalizedKeyword, normalizedCategory, () -> activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                .eq(Activity::getStatus, ActivityStatus.PUBLISHED)
                .eq(normalizedCategory != null && !normalizedCategory.isBlank(), Activity::getCategory, normalizedCategory)
                .like(normalizedKeyword != null && !normalizedKeyword.isEmpty(), Activity::getTitle,
                        normalizedKeyword)
                .orderByDesc(Activity::getCreatedAt)));
        if (activities.isEmpty()) return List.of();
        LocalDateTime now = LocalDateTime.now();
        List<ActivitySession> onSaleSessions = activitySessionMapper.selectList(
                Wrappers.<ActivitySession>lambdaQuery()
                        .in(ActivitySession::getActivityId, activities.stream().map(Activity::getId).toList())
                        .eq(ActivitySession::getStatus, SessionStatus.ON_SALE)
                        .orderByAsc(ActivitySession::getStartTime));
        onSaleSessions = onSaleSessions.stream().filter(session -> hasNotEnded(session, now)).toList();
        Map<Long, ActivitySession> sessionsById = onSaleSessions.stream()
                .collect(Collectors.toMap(ActivitySession::getId, session -> session, (first, ignored) -> first, LinkedHashMap::new));
        Map<Long, Long> sessionActivities = onSaleSessions.stream()
                .collect(Collectors.toMap(ActivitySession::getId, ActivitySession::getActivityId));
        Map<Long, List<TicketType>> visibleTypes = new HashMap<>();
        Map<Long, ActivitySession> visibleSessions = new HashMap<>();
        if (!sessionActivities.isEmpty()) {
            for (TicketType type : ticketTypeMapper.selectList(Wrappers.<TicketType>lambdaQuery()
                    .in(TicketType::getSessionId, sessionActivities.keySet())
                    .eq(TicketType::getEnabled, true))) {
                if (!isVisibleSale(type, now)) continue;
                Long activityId = sessionActivities.get(type.getSessionId());
                if (activityId != null && type.getPrice() != null) {
                    visibleTypes.computeIfAbsent(activityId, ignored -> new ArrayList<>()).add(type);
                    ActivitySession session = sessionsById.get(type.getSessionId());
                    if (session != null) {
                        ActivitySession current = visibleSessions.get(activityId);
                        if (current == null || session.getStartTime().isBefore(current.getStartTime())) {
                            visibleSessions.put(activityId, session);
                        }
                    }
                }
            }
        }
        return activities.stream().map(activity -> {
            UserActivitySummaryVO summary = UserActivitySummaryVO.from(activity, fileUrl(activity.getCoverFileId()));
            populateSaleSummary(summary, visibleTypes.getOrDefault(activity.getId(), List.of()), now);
            ActivitySession session = visibleSessions.get(activity.getId());
            if (session != null) {
                summary.setStartTime(session.getStartTime());
                summary.setEndTime(session.getEndTime());
            }
            return summary;
        }).filter(summary -> summary.getStartTime() != null).toList();
    }

    @Override
    public UserActivityDetailVO getDetail(Long activityId) {
        Activity activity = publicActivityCache.getPublished(activityId);
        if (activity == null || activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(SystemResultCode.NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        List<UserActivitySessionVO> sessions = activitySessionMapper.selectList(
                        Wrappers.<ActivitySession>lambdaQuery()
                                .eq(ActivitySession::getActivityId, activityId)
                                .eq(ActivitySession::getStatus, SessionStatus.ON_SALE)
                                .orderByAsc(ActivitySession::getStartTime))
                .stream().filter(session -> hasNotEnded(session, now)).map(session -> UserActivitySessionVO.from(session, findEnabledTicketTypes(session.getId(), now)))
                .filter(session -> !session.getTicketTypes().isEmpty())
                .toList();
        if (sessions.isEmpty()) throw new BusinessException(SystemResultCode.NOT_FOUND);
        return UserActivityDetailVO.from(activity, fileUrl(activity.getCoverFileId()), sessions);
    }

    private List<UserTicketTypeVO> findEnabledTicketTypes(Long sessionId, LocalDateTime now) {
        return ticketTypeMapper.selectList(Wrappers.<TicketType>lambdaQuery()
                        .eq(TicketType::getSessionId, sessionId)
                        .eq(TicketType::getEnabled, true)
                        .orderByAsc(TicketType::getId))
                .stream().filter(type -> isVisibleSale(type, now)).map(UserTicketTypeVO::from).toList();
    }

    @Override
    public UserActivityPreviewVO getPreview(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(SystemResultCode.NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        List<ActivitySession> sessions = activitySessionMapper.selectList(Wrappers.<ActivitySession>lambdaQuery()
                        .eq(ActivitySession::getActivityId, activityId).orderByAsc(ActivitySession::getStartTime))
                .stream().filter(session -> session.getStatus() == SessionStatus.ON_SALE || session.getStatus() == SessionStatus.CLOSED).toList();
        UserActivitySummaryVO summary = UserActivitySummaryVO.from(activity, fileUrl(activity.getCoverFileId()));
        List<ActivitySession> future = sessions.stream()
                .filter(session -> session.getStatus() == SessionStatus.ON_SALE && hasNotEnded(session, now)).toList();
        ActivitySession display = future.stream().min(Comparator.comparing(ActivitySession::getStartTime))
                .orElse(sessions.stream().max(Comparator.comparing(ActivitySession::getStartTime)).orElse(null));
        if (display != null) {
            summary.setStartTime(display.getStartTime());
            summary.setEndTime(display.getEndTime());
        }
        String availability = "UNAVAILABLE_SALE";
        if (!sessions.isEmpty() && sessions.stream().allMatch(session -> session.getEndTime() != null && !session.getEndTime().isAfter(now))) {
            availability = "ENDED";
        } else if (!future.isEmpty()) {
            List<TicketType> types = ticketTypeMapper.selectList(Wrappers.<TicketType>lambdaQuery()
                    .in(TicketType::getSessionId, future.stream().map(ActivitySession::getId).toList())
                    .eq(TicketType::getEnabled, true));
            populateSaleSummary(summary, types, now);
            availability = summary.getAvailability();
        }
        summary.setAvailability(availability);
        return new UserActivityPreviewVO(summary, availability);
    }

    private void populateSaleSummary(UserActivitySummaryVO summary, List<TicketType> types, LocalDateTime now) {
        List<TicketType> visible = types.stream().filter(type -> isVisibleSale(type, now)).toList();
        List<TicketType> open = visible.stream().filter(type -> isSaleOpen(type, now)).toList();
        LocalDateTime opening = visible.stream().map(TicketType::getSaleStartTime)
                .filter(time -> time.isAfter(now)).min(LocalDateTime::compareTo).orElse(null);
        String availability = open.stream().anyMatch(type -> type.getStock() != null && type.getStock() > 0)
                ? "AVAILABLE" : opening != null ? "UPCOMING" : !open.isEmpty() ? "SOLD_OUT" : "UNAVAILABLE_SALE";
        summary.setAvailability(availability);
        summary.setSaleStartTime(opening);
        List<TicketType> priced = "UPCOMING".equals(availability)
                ? visible.stream().filter(type -> type.getSaleStartTime().isAfter(now)).toList() : open;
        summary.setMinPrice(priced.stream().map(TicketType::getPrice).filter(Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(null));
    }

    private boolean isVisibleSale(TicketType type, LocalDateTime now) {
        return Boolean.TRUE.equals(type.getEnabled()) && type.getSaleStartTime() != null
                && type.getSaleEndTime() != null && type.getSaleEndTime().isAfter(type.getSaleStartTime())
                && !now.isAfter(type.getSaleEndTime());
    }

    private boolean hasNotEnded(ActivitySession session, LocalDateTime now) {
        return session.getEndTime() != null && session.getEndTime().isAfter(now);
    }

    private boolean isSaleOpen(TicketType type, LocalDateTime now) {
        return isVisibleSale(type, now) && !now.isBefore(type.getSaleStartTime());
    }

    private String fileUrl(Long fileId) {
        return fileStorageService == null ? null : fileStorageService.getUrl(fileId);
    }
}
