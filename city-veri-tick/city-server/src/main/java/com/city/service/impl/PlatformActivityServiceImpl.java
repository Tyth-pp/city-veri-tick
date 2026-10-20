package com.city.service.impl;

import com.city.constant.ActivityMessageConstant;
import com.city.constant.ManagementMessageConstant;
import com.city.cache.ActivityListCache;
import com.city.service.ManagementMessageService;
import com.city.cache.PublicActivityCache;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import com.city.mapper.OrganizerMapper;
import com.city.mapper.UserMapper;
import com.city.result.PageResult;
import com.city.service.PlatformActivityService;
import com.city.vo.ActivityViewVO;
import com.city.vo.ActivitySessionViewVO;
import com.city.vo.PlatformActivityDetailVO;
import com.city.vo.PlatformTicketTypeVO;
import com.city.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import com.city.utils.SearchTextUtils;

@Service
public class PlatformActivityServiceImpl implements PlatformActivityService {
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private ActivitySessionMapper activitySessionMapper;
    @Autowired
    private TicketTypeMapper ticketTypeMapper;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private OrganizerMapper organizerMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ManagementMessageService messages;
    @Autowired
    private PublicActivityCache publicActivityCache;
    @Autowired
    private ActivityListCache activityListCache;

    @Override
    public List<ActivityViewVO> list() {
        return activityMapper.selectList(Wrappers.<Activity>lambdaQuery().orderByDesc(Activity::getCreatedAt))
                .stream().map(activity -> ActivityViewVO.from(activity, fileUrl(activity.getCoverFileId()))).toList();
    }

    @Override
    public PageResult<ActivityViewVO> page(String keyword, String status, long page, long pageSize) {
        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        List<Activity> all = activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                .orderByDesc(Activity::getCreatedAt));
        String normalizedKeyword = SearchTextUtils.normalizeKeyword(keyword);
        List<ActivityViewVO> filtered = all.stream()
                .filter(activity -> status == null || status.isBlank()
                        || activity.getStatus().name().equalsIgnoreCase(status.trim()))
                .filter(activity -> normalizedKeyword.isBlank()
                        || SearchTextUtils.containsKeyword(activity.getTitle(), normalizedKeyword)
                        || SearchTextUtils.containsKeyword(activity.getVenueName(), normalizedKeyword)
                        || String.valueOf(activity.getId()).contains(normalizedKeyword))
                .map(activity -> ActivityViewVO.from(activity, fileUrl(activity.getCoverFileId())))
                .toList();
        int from = (int) Math.min(filtered.size(), (safePage - 1) * safePageSize);
        int to = (int) Math.min(filtered.size(), from + safePageSize);
        List<ActivityViewVO> pageItems = filtered.subList(from, to);
        for (ActivityViewVO view : pageItems) {
            if (view.getOrganizerId() != null) {
                var organizer = organizerMapper.selectById(view.getOrganizerId());
                if (organizer != null) view.setOrganizerName(organizer.getName());
            }
            if (view.getSubmittedBy() != null) {
                var submitter = userMapper.selectById(view.getSubmittedBy());
                if (submitter != null)
                    view.setSubmitterName(submitter.getStaffName() == null ? submitter.getUsername() : submitter.getStaffName());
            }
        }
        return new PageResult<>(pageItems, filtered.size(), safePage, safePageSize);
    }

    @Override
    public PlatformActivityDetailVO detail(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(SystemResultCode.NOT_FOUND);
        }
        List<ActivitySession> sessionEntities = activitySessionMapper.selectList(
                Wrappers.<ActivitySession>lambdaQuery().eq(ActivitySession::getActivityId, activityId)
                        .orderByAsc(ActivitySession::getStartTime));
        List<ActivitySessionViewVO> sessions = sessionEntities.stream()
                .map(ActivitySessionViewVO::from).toList();
        List<Long> sessionIds = sessionEntities.stream().map(ActivitySession::getId).toList();
        List<PlatformTicketTypeVO> ticketTypes = sessionIds.isEmpty()
                ? List.of()
                : ticketTypeMapper.selectList(Wrappers.<TicketType>lambdaQuery()
                        .in(TicketType::getSessionId, sessionIds).orderByAsc(TicketType::getSessionId))
                .stream().map(PlatformTicketTypeVO::from).toList();
        PlatformActivityDetailVO detail = PlatformActivityDetailVO.of(activity, fileUrl(activity.getCoverFileId()), sessions, ticketTypes);
        if (activity.getOrganizerId() != null) {
            detail.setOrganizer(PlatformActivityDetailVO.OrganizerInfo.from(organizerMapper.selectById(activity.getOrganizerId())));
        }
        if (activity.getSubmittedBy() != null) {
            detail.setSubmitter(PlatformActivityDetailVO.AccountInfo.from(userMapper.selectById(activity.getSubmittedBy())));
        }
        if (activity.getReviewedBy() != null) {
            detail.setReviewer(PlatformActivityDetailVO.AccountInfo.from(userMapper.selectById(activity.getReviewedBy())));
        }
        return detail;
    }

    private String fileUrl(Long fileId) {
        return fileStorageService == null || fileId == null ? null : fileStorageService.getUrl(fileId);
    }

    @Override
    @Transactional
    public void approve(Long reviewerId, Long activityId) {
        review(reviewerId, activityId, ActivityStatus.PENDING, ActivityStatus.PUBLISHED, null);
        updateSessionStatus(activityId, SessionStatus.DRAFT, SessionStatus.ON_SALE);
    }

    @Override
    @Transactional
    public void reject(Long reviewerId, Long activityId, String reason) {
        review(reviewerId, activityId, ActivityStatus.PENDING, ActivityStatus.REJECTED, reason);
    }

    @Override
    @Transactional
    public void offline(Long reviewerId, Long activityId) {
        review(reviewerId, activityId, ActivityStatus.PUBLISHED, ActivityStatus.OFFLINE, null);
        updateSessionStatus(activityId, SessionStatus.ON_SALE, SessionStatus.CLOSED);
    }

    @Override
    @Transactional
    public void offline(Long reviewerId, Long activityId, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 500)
            throw new IllegalArgumentException(ActivityMessageConstant.OFFLINE_REASON_REQUIRED);
        review(reviewerId, activityId, ActivityStatus.PUBLISHED, ActivityStatus.OFFLINE, reason.trim());
        updateSessionStatus(activityId, SessionStatus.ON_SALE, SessionStatus.CLOSED);
    }

    private void review(Long reviewerId, Long activityId, ActivityStatus source, ActivityStatus target, String reason) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(SystemResultCode.NOT_FOUND);
        }
        int updated = activityMapper.update(null, Wrappers.<Activity>lambdaUpdate()
                .eq(Activity::getId, activityId)
                .eq(Activity::getStatus, source)
                .set(Activity::getStatus, target)
                .set(Activity::getRejectionReason, reason)
                .set(Activity::getReviewedBy, reviewerId)
                .set(Activity::getReviewedAt, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        publicActivityCache.invalidateAfterCommit(activityId);
        activityListCache.invalidateAfterCommit();
        String title = target == ActivityStatus.PUBLISHED ? ManagementMessageConstant.ACTIVITY_PUBLISHED : target == ActivityStatus.REJECTED ? ManagementMessageConstant.ACTIVITY_REJECTED : ManagementMessageConstant.ACTIVITY_OFFLINE;
        messages.publish("ORGANIZER_ADMIN", activity.getOrganizerId(), "ACTIVITY_" + target.name(), title,
                ManagementMessageConstant.ACTIVITY_CONTENT_TEMPLATE.formatted(activity.getTitle(),
                        reason == null || reason.isBlank() ? ManagementMessageConstant.ACTIVITY_DETAILS_SUFFIX : ManagementMessageConstant.ACTIVITY_REASON_TEMPLATE.formatted(reason)),
                "/management/organizer/activities/" + activityId + "/edit");
    }

    private void updateSessionStatus(Long activityId, SessionStatus source, SessionStatus target) {
        activitySessionMapper.update(null, new UpdateWrapper<ActivitySession>()
                .eq("activity_id", activityId)
                .eq("status", source.name())
                .set("status", target.name()));
    }
}
