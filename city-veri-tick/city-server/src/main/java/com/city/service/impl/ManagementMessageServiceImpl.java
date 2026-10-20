package com.city.service.impl;

import com.city.constant.ManagementMessageConstant;
import com.city.service.ManagementMessageService;
import com.city.entity.Activity;
import org.springframework.transaction.annotation.Propagation;
import com.city.mapper.ManagementMessageMapper;
import com.city.security.LoginPrincipal;
import com.city.service.OrganizerAccessService;
import com.city.result.SystemResultCode;
import com.city.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class ManagementMessageServiceImpl implements ManagementMessageService {
    private final ManagementMessageMapper mapper;
    private final OrganizerAccessService access;
    private final Clock clock;

    public ManagementMessageServiceImpl(ManagementMessageMapper mapper, OrganizerAccessService access, Clock clock) {
        this.mapper = mapper;
        this.access = access;
        this.clock = clock;
    }

    private Long scope(LoginPrincipal user) {
        if ("PLATFORM_ADMIN".equals(user.role())) return null;
        if ("ORGANIZER_ADMIN".equals(user.role())) return access.requireOrganizerId(user.userId());
        throw new BusinessException(SystemResultCode.FORBIDDEN);
    }

    @Override
    public Map<String, Object> inbox(LoginPrincipal user, int page) {
        Long org = scope(user);
        int safe = Math.min(10000, Math.max(1, page));
        Map<String, Object> result = new HashMap<>();
        result.put("items", mapper.list(user.role(), org, user.userId(), (safe - 1) * 20));
        result.put("unread", mapper.unread(user.role(), org, user.userId()));
        var ids = mapper.unreadIds(user.role(), org, user.userId());
        result.put("unreadIds", ids);
        result.put("unreadCount", ids.size());
        result.put("page", safe);
        return result;
    }

    @Override
    public void read(LoginPrincipal user, Long id) {
        Long org = scope(user);
        if (mapper.visible(user.role(), org, id) != 1) throw new BusinessException(SystemResultCode.NOT_FOUND);
        mapper.markRead(id, user.userId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void submission(Activity activity, Long userId) {
        var label = mapper.submissionLabel(activity.getOrganizerId(), userId);
        String content = ManagementMessageConstant.SUBMISSION_CONTENT_TEMPLATE.formatted(activity.getTitle(),
                label == null ? ManagementMessageConstant.ORGANIZER_ID_TEMPLATE.formatted(activity.getOrganizerId()) : label.get("organizerName"),
                label == null ? ManagementMessageConstant.ACCOUNT_ID_TEMPLATE.formatted(userId) : label.get("submitterName"));
        publish("PLATFORM_ADMIN", null, "ACTIVITY_SUBMITTED", activity.getSubmittedAt() == null ? ManagementMessageConstant.ACTIVITY_SUBMITTED : ManagementMessageConstant.ACTIVITY_RESUBMITTED, content, "/management/platform/activities/" + activity.getId());
    }

    // The event is the durable inbox record. It commits/rolls back with the business transition.
    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void publish(String audience, Long organizerId, String type, String title, String content, String path) {
        if (mapper.insert(audience, organizerId, type, title, content, path, LocalDateTime.now(clock)) != 1)
            throw new IllegalStateException(ManagementMessageConstant.MESSAGE_SAVE_FAILED);
    }
}
