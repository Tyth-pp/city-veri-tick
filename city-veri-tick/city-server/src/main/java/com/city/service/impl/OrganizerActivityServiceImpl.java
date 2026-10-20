package com.city.service.impl;
import com.city.cache.ActivityListCache;
import com.city.service.ManagementMessageService;
import com.city.cache.PublicActivityCache;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.city.result.SystemResultCode;
import com.city.dto.CreateActivityDTO;
import com.city.dto.UpdateActivityDTO;
import com.city.entity.Activity;
import com.city.entity.ActivitySession;
import com.city.entity.TicketType;
import com.city.enums.ActivityStatus;
import com.city.exception.BusinessException;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivitySessionMapper;
import com.city.mapper.TicketTypeMapper;
import com.city.service.OrganizerAccessService;
import com.city.service.OrganizerActivityService;
import com.city.service.FileStorageService;
import com.city.vo.ActivityViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizerActivityServiceImpl implements OrganizerActivityService {
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private ActivitySessionMapper activitySessionMapper;
    @Autowired
    private TicketTypeMapper ticketTypeMapper;
    @Autowired
    private OrganizerAccessService organizerAccessService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired private ManagementMessageService messages;
    @Autowired private PublicActivityCache publicActivityCache;
    @Autowired private ActivityListCache activityListCache;

    @Override
    @Transactional
    public ActivityViewVO create(Long userId, CreateActivityDTO dto) {
        Activity activity = new Activity();
        activity.setOrganizerId(organizerAccessService.requireOrganizerId(userId));
        activity.setCreatedBy(userId);
        activity.setStatus(ActivityStatus.DRAFT);
        copy(dto, activity);
        activityMapper.insert(activity);
        publicActivityCache.invalidateAfterCommit(activity.getId());
        activityListCache.invalidateAfterCommit();
        return ActivityViewVO.from(activity, fileUrl(activity.getCoverFileId()));
    }

    @Override
    @Transactional
    public ActivityViewVO update(Long userId, Long activityId, UpdateActivityDTO dto) {
        Activity activity = requireOwned(userId, activityId);
        if (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.REJECTED) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        int updated = activityMapper.update(null, Wrappers.<Activity>lambdaUpdate()
                .eq(Activity::getId, activityId)
                .eq(Activity::getOrganizerId, activity.getOrganizerId())
                .in(Activity::getStatus, ActivityStatus.DRAFT, ActivityStatus.REJECTED)
                .set(Activity::getTitle, dto.getTitle())
                .set(Activity::getCategory, dto.getCategory())
                .set(Activity::getDescription, dto.getDescription() != null ? dto.getDescription() : dto.getContent())
                .set(Activity::getContent, dto.getContent())
                .set(Activity::getHighlights, dto.getHighlights())
                .set(Activity::getNotices, dto.getNotices())
                .set(Activity::getCoverFileId, dto.getCoverFileId())
                .set(Activity::getVenueName, dto.getVenueName())
                .set(Activity::getVenueAddress, dto.getVenueAddress())
                .set(Activity::getStatus, ActivityStatus.DRAFT)
                .set(Activity::getRejectionReason, null)
                .set(Activity::getReviewedBy, null)
                .set(Activity::getReviewedAt, null));
        if (updated != 1) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        publicActivityCache.invalidateAfterCommit(activityId);
        activityListCache.invalidateAfterCommit();
        copy(dto, activity);
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setRejectionReason(null);
        activity.setReviewedBy(null);
        activity.setReviewedAt(null);
        return ActivityViewVO.from(activity, fileUrl(activity.getCoverFileId()));
    }

    @Override
    @Transactional
    public void submit(Long userId, Long activityId) {
        Activity activity = requireOwned(userId, activityId);
        if (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.REJECTED) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        List<ActivitySession> sessions = activitySessionMapper.selectList(
                new QueryWrapper<ActivitySession>().eq("activity_id", activityId));
        if (sessions.isEmpty()) {
            throw new BusinessException(SystemResultCode.ACTIVITY_SESSION_REQUIRED);
        }
        boolean missingTicketType = sessions.stream().anyMatch(session -> ticketTypeMapper.selectCount(
                new QueryWrapper<TicketType>().eq("session_id", session.getId()).eq("enabled", true)) == 0);
        if (missingTicketType) {
            throw new BusinessException(SystemResultCode.TICKET_TYPE_REQUIRED);
        }
        int updated = activityMapper.update(null, Wrappers.<Activity>lambdaUpdate()
                .eq(Activity::getId, activityId)
                .eq(Activity::getOrganizerId, activity.getOrganizerId())
                .in(Activity::getStatus, ActivityStatus.DRAFT, ActivityStatus.REJECTED)
                .set(Activity::getStatus, ActivityStatus.PENDING)
                .set(Activity::getSubmittedBy, userId)
                .set(Activity::getSubmittedAt, LocalDateTime.now())
                .set(Activity::getRejectionReason, null)
                .set(Activity::getReviewedBy, null)
                .set(Activity::getReviewedAt, null));
        if (updated != 1) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        publicActivityCache.invalidateAfterCommit(activityId);
        activityListCache.invalidateAfterCommit();
        messages.submission(activity,userId);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long activityId) {
        Activity activity = requireOwned(userId, activityId);
        if (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.REJECTED) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        int deleted = activityMapper.delete(Wrappers.<Activity>lambdaQuery()
                .eq(Activity::getId, activityId)
                .eq(Activity::getOrganizerId, activity.getOrganizerId())
                .in(Activity::getStatus, ActivityStatus.DRAFT, ActivityStatus.REJECTED));
        if (deleted != 1) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        publicActivityCache.invalidateAfterCommit(activityId);
        activityListCache.invalidateAfterCommit();
    }

    @Override
    public List<ActivityViewVO> listMine(Long userId) {
        Long organizerId = organizerAccessService.requireOrganizerId(userId);
        return activityMapper.selectList(Wrappers.<Activity>lambdaQuery()
                        .eq(Activity::getOrganizerId, organizerId)
                        .orderByDesc(Activity::getCreatedAt))
                .stream().map(activity -> ActivityViewVO.from(activity, fileUrl(activity.getCoverFileId()))).toList();
    }

    private Activity requireOwned(Long userId, Long activityId) {
        Long organizerId = organizerAccessService.requireOrganizerId(userId);
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(SystemResultCode.NOT_FOUND);
        }
        if (!organizerId.equals(activity.getOrganizerId())) {
            throw new BusinessException(SystemResultCode.FORBIDDEN);
        }
        return activity;
    }

    private void copy(CreateActivityDTO dto, Activity activity) {
        activity.setTitle(dto.getTitle());
        activity.setCategory(dto.getCategory());
        activity.setDescription(legacyDescription(dto));
        activity.setContent(dto.getContent());
        activity.setHighlights(dto.getHighlights());
        activity.setNotices(dto.getNotices());
        activity.setCoverFileId(dto.getCoverFileId());
        activity.setVenueName(dto.getVenueName());
        activity.setVenueAddress(dto.getVenueAddress());
    }

    private void copy(UpdateActivityDTO dto, Activity activity) {
        activity.setTitle(dto.getTitle());
        activity.setCategory(dto.getCategory());
        activity.setDescription(dto.getDescription() != null ? dto.getDescription() : dto.getContent());
        activity.setContent(dto.getContent());
        activity.setHighlights(dto.getHighlights());
        activity.setNotices(dto.getNotices());
        activity.setCoverFileId(dto.getCoverFileId());
        activity.setVenueName(dto.getVenueName());
        activity.setVenueAddress(dto.getVenueAddress());
    }

    private String legacyDescription(CreateActivityDTO dto) {
        return dto.getDescription() != null ? dto.getDescription() : dto.getContent();
    }

    private String fileUrl(Long fileId) {
        return fileStorageService == null ? null : fileStorageService.getUrl(fileId);
    }
}
