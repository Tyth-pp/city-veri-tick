package com.city.service.impl;

import com.city.entity.OrganizerMember;
import com.city.mapper.OrganizerMemberMapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.result.SystemResultCode;
import com.city.dto.AssignVerifierDTO;
import com.city.entity.Activity;
import com.city.entity.ActivityVerifier;
import com.city.entity.User;
import com.city.exception.BusinessException;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivityVerifierMapper;
import com.city.mapper.UserMapper;
import com.city.service.PlatformVerifierService;
import com.city.vo.PlatformVerifierViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformVerifierServiceImpl implements PlatformVerifierService {
    @Autowired
    private ActivityVerifierMapper verifierMapper;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrganizerMemberMapper members;

    private void requireMembership(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        User user = userMapper.selectById(userId);
        if (activity == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled()) || userMapper.countRole(userId, "VERIFIER") == 0
                || members.selectCount(Wrappers.<OrganizerMember>lambdaQuery()
                .eq(OrganizerMember::getUserId, userId)
                .eq(OrganizerMember::getOrganizerId, activity.getOrganizerId())
                .eq(OrganizerMember::getEnabled, true)) == 0)
            throw new BusinessException(SystemResultCode.FORBIDDEN);
    }

    @Override
    public List<PlatformVerifierViewVO> list(Long activityId, Boolean enabled) {
        Map<Long, Activity> activities = new HashMap<>();
        Map<Long, User> users = new HashMap<>();
        return verifierMapper.selectList(Wrappers.<ActivityVerifier>lambdaQuery()
                        .eq(activityId != null, ActivityVerifier::getActivityId, activityId)
                        .eq(enabled != null, ActivityVerifier::getEnabled, enabled)
                        .orderByDesc(ActivityVerifier::getCreatedAt))
                .stream().map(item -> toView(item, activities, users)).toList();
    }

    @Override
    @Transactional
    public void assign(Long assignedBy, AssignVerifierDTO dto) {
        requireMembership(dto.getActivityId(), dto.getUserId());
        if (activityMapper.selectById(dto.getActivityId()) == null)
            throw new BusinessException(SystemResultCode.NOT_FOUND);
        if (userMapper.selectById(dto.getUserId()) == null || userMapper.countRole(dto.getUserId(), "VERIFIER") == 0) {
            throw new BusinessException(SystemResultCode.FORBIDDEN);
        }
        ActivityVerifier existing = verifierMapper.selectOne(Wrappers.<ActivityVerifier>lambdaQuery()
                .eq(ActivityVerifier::getActivityId, dto.getActivityId()).eq(ActivityVerifier::getUserId, dto.getUserId()));
        if (existing == null) {
            existing = new ActivityVerifier();
            existing.setActivityId(dto.getActivityId());
            existing.setUserId(dto.getUserId());
        }
        existing.setAssignedBy(assignedBy);
        existing.setEnabled(true);
        existing.setValidFrom(dto.getValidFrom());
        existing.setValidUntil(dto.getValidUntil());
        if (existing.getId() == null) verifierMapper.insert(existing);
        else verifierMapper.updateById(existing);
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        ActivityVerifier verifier = verifierMapper.selectById(id);
        if (verifier == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        if (enabled) requireMembership(verifier.getActivityId(), verifier.getUserId());
        verifier.setEnabled(enabled);
        verifierMapper.updateById(verifier);
    }

    private PlatformVerifierViewVO toView(ActivityVerifier item, Map<Long, Activity> activities, Map<Long, User> users) {
        Activity activity = activities.computeIfAbsent(item.getActivityId(), activityMapper::selectById);
        User user = users.computeIfAbsent(item.getUserId(), userMapper::selectById);
        PlatformVerifierViewVO view = new PlatformVerifierViewVO();
        view.setId(item.getId());
        view.setActivityId(item.getActivityId());
        view.setActivityTitle(activity == null ? null : activity.getTitle());
        view.setUserId(item.getUserId());
        if (user != null) {
            view.setUsername(user.getUsername());
            view.setNickname(user.getNickname());
            view.setName(user.getStaffName());
            view.setPhone(user.getPhone());
        }
        view.setEnabled(item.getEnabled());
        view.setAssignedBy(item.getAssignedBy());
        view.setValidFrom(item.getValidFrom());
        view.setValidUntil(item.getValidUntil());
        view.setCreatedAt(item.getCreatedAt());
        return view;
    }
}
