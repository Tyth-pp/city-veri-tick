package com.city.service.impl;
import com.city.service.AccountOperationService;
import com.city.service.StaffAccountService;
import com.city.service.support.AccountOperationPolicy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.result.SystemResultCode;
import com.city.dto.*;
import com.city.entity.*;
import com.city.exception.BusinessException;
import com.city.mapper.*;
import com.city.service.OrganizerAccessService;
import com.city.security.*;
import com.city.vo.StaffAccountViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class StaffAccountServiceImpl implements StaffAccountService {
    @Autowired
    private UserMapper users;
    @Autowired
    private OrganizerMapper organizers;
    @Autowired
    private OrganizerMemberMapper members;
    @Autowired
    private ActivityMapper activities;
    @Autowired
    private ActivityVerifierMapper assignments;
    @Autowired
    private OrganizerAccessService access;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private TokenBlacklistService tokens;
    @Autowired
    private AccountOperationService accountOperations;

    @Transactional
    @Override
    public Long createOrganizer(Long actor, CreateOrganizerDTO dto) {
        Organizer org = new Organizer();
        org.setName(dto.getName().trim());
        org.setContactName(dto.getContactName().trim());
        org.setContactPhone(dto.getContactPhone());
        String credit = dto.getUnifiedSocialCreditCode();
        if (credit != null && !credit.isBlank()) {
            credit = credit.trim();
            if (organizers.selectCount(Wrappers.<Organizer>lambdaQuery().eq(Organizer::getUnifiedSocialCreditCode, credit)) > 0)
                throw new BusinessException(SystemResultCode.CONFLICT);
            org.setUnifiedSocialCreditCode(credit);
        }
        // Direct platform provisioning is approval by the authenticated platform administrator.
        org.setStatus("APPROVED");
        org.setReviewedBy(actor);
        org.setReviewedAt(LocalDateTime.now());
        organizers.insert(org);
        createAccount(org.getId(), dto.getAdministrator(), "ORGANIZER_ADMIN", "OWNER");
        return org.getId();
    }

    @Transactional
    @Override
    public Long createAdministrator(Long organizerId, StaffAccountDTO dto) {
        requireOrganization(organizerId);
        return createAccount(organizerId, dto, "ORGANIZER_ADMIN", "OWNER");
    }

    @Transactional
    @Override
    public Long createVerifier(Long actor, StaffAccountDTO dto) {
        Long org = access.requireOrganizerId(actor);
        requireOrganization(org);
        return createAccount(org, dto, "VERIFIER", "VERIFIER");
    }

    private Organizer requireOrganization(Long id) {
        Organizer org = organizers.selectById(id);
        if (org == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        if (!"APPROVED".equals(org.getStatus())) throw new BusinessException(SystemResultCode.CONFLICT);
        return org;
    }

    private Long createAccount(Long org, StaffAccountDTO dto, String role, String memberRole) {
        if (users.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername())) > 0)
            throw new BusinessException(SystemResultCode.ACCOUNT_DUPLICATE);
        String phone = dto.getPhone();
        if (phone != null && !phone.isBlank() && users.selectByPhone(phone) != null)
            throw new BusinessException(SystemResultCode.ACCOUNT_DUPLICATE);
        Long roleId = users.selectRoleId(role);
        if (roleId == null) throw new BusinessException(SystemResultCode.INTERNAL_ERROR);
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getName().trim());
        user.setStaffName(dto.getName().trim());
        user.setPhone(phone == null || phone.isBlank() ? null : phone);
        user.setPasswordHash(encoder.encode(dto.getInitialPassword()));
        user.setEnabled(true);
        user.setMustChangePassword(true);
        users.insert(user);
        users.insertRole(user.getId(), roleId);
        OrganizerMember member = new OrganizerMember();
        member.setOrganizerId(org);
        member.setUserId(user.getId());
        member.setMemberRole(memberRole);
        member.setEnabled(true);
        members.insert(member);
        return user.getId();
    }

    @Override
    public List<StaffAccountViewVO> accounts(Long org, String role) {
        return members.selectList(Wrappers.<OrganizerMember>lambdaQuery().eq(OrganizerMember::getOrganizerId, org))
                .stream().filter(m -> role == null || users.countRole(m.getUserId(), role) > 0)
                .map(m -> {
                    User u = users.selectById(m.getUserId());
                    if (u == null) return null;
                    StaffAccountViewVO v = new StaffAccountViewVO();
                    v.setId(u.getId());
                    v.setUsername(u.getUsername());
                    v.setNickname(u.getNickname());
                    v.setPhone(u.getPhone());
                    v.setEnabled(u.getEnabled());
                    v.setName(u.getStaffName());
                    v.setRole(users.countRole(u.getId(), "ORGANIZER_ADMIN") > 0 ? "ORGANIZER_ADMIN" : "VERIFIER");
                    v.setActivityIds(assignments.selectList(Wrappers.<ActivityVerifier>lambdaQuery()
                                    .eq(ActivityVerifier::getUserId, u.getId()).eq(ActivityVerifier::getEnabled, true)).stream()
                            .filter(a -> {
                                Activity activity = activities.selectById(a.getActivityId());
                                return activity != null && org.equals(activity.getOrganizerId());
                            })
                            .map(ActivityVerifier::getActivityId).toList());
                    return v;
                }).filter(Objects::nonNull).toList();
    }

    @Override
    public List<StaffAccountViewVO> verifierAccounts(Long actor) {
        return accounts(access.requireOrganizerId(actor), "VERIFIER");
    }

    @Override
    public List<Map<String, Object>> activityOptions(Long org) {
        return activities.selectList(Wrappers.<Activity>lambdaQuery().eq(Activity::getOrganizerId, org).orderByDesc(Activity::getCreatedAt))
                .stream().map(a -> {
                    Map<String, Object> v = new LinkedHashMap<>();
                    v.put("id", a.getId());
                    v.put("title", a.getTitle());
                    v.put("status", a.getStatus());
                    return v;
                }).toList();
    }

    @Override
    public List<Map<String, Object>> verifierActivityOptions(Long actor) {
        return activityOptions(access.requireOrganizerId(actor));
    }

    private Long requireVerifier(Long actor, Long userId) {
        Long org = access.requireOrganizerId(actor);
        OrganizerMember m = members.selectOne(Wrappers.<OrganizerMember>lambdaQuery()
                .eq(OrganizerMember::getOrganizerId, org).eq(OrganizerMember::getUserId, userId).eq(OrganizerMember::getEnabled, true));
        if (m == null || users.countRole(userId, "VERIFIER") == 0 || users.countRole(userId, "PLATFORM_ADMIN") > 0
                || users.countRole(userId, "ORGANIZER_ADMIN") > 0)
            throw new BusinessException(SystemResultCode.FORBIDDEN);
        return org;
    }

    @Transactional
    @Override
    public void setVerifierEnabled(Long actor, Long userId, boolean enabled) {
        setVerifierEnabled(actor, userId, enabled, null);
    }

    @Transactional
    @Override
    public void setVerifierEnabled(Long actor, Long userId, boolean enabled, String reason) {
        requireVerifier(actor, userId);
        String detail = AccountOperationPolicy.validate(reason, !enabled);
        if (users.selectByIdForUpdate(userId) == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        if (!enabled) tokens.revokeAll(userId, RevocationReason.ACCOUNT_DISABLED);
        User patch = new User();
        patch.setId(userId);
        patch.setEnabled(enabled);
        if (users.updateById(patch) != 1) throw new BusinessException(SystemResultCode.NOT_FOUND);
        accountOperations.record(actor, userId, enabled ? "ENABLE" : "DISABLE", detail);
    }

    @Transactional
    @Override
    public void setVerifierActivities(Long actor, Long userId, VerifierActivitiesDTO dto) {
        Long org = requireVerifier(actor, userId);
        Set<Long> selected = new LinkedHashSet<>(dto.getActivityIds());
        for (Long id : selected) {
            Activity a = activities.selectById(id);
            if (a == null || !org.equals(a.getOrganizerId())) throw new BusinessException(SystemResultCode.FORBIDDEN);
        }
        // Disable only this organization's assignments. Historical check-in records are preserved.
        List<Long> ownIds = activities.selectList(Wrappers.<Activity>lambdaQuery().eq(Activity::getOrganizerId, org)).stream().map(Activity::getId).toList();
        if (!ownIds.isEmpty()) assignments.update(null, Wrappers.<ActivityVerifier>lambdaUpdate()
                .eq(ActivityVerifier::getUserId, userId).in(ActivityVerifier::getActivityId, ownIds).set(ActivityVerifier::getEnabled, false));
        for (Long id : selected) {
            ActivityVerifier a = assignments.selectOne(Wrappers.<ActivityVerifier>lambdaQuery().eq(ActivityVerifier::getUserId, userId).eq(ActivityVerifier::getActivityId, id));
            if (a == null) {
                a = new ActivityVerifier();
                a.setUserId(userId);
                a.setActivityId(id);
            }
            a.setEnabled(true);
            a.setAssignedBy(actor);
            // Clearing prior windows is intentional: this form grants unrestricted activity access.
            if (a.getId() == null) assignments.insert(a);
            else
                assignments.update(null, Wrappers.<ActivityVerifier>lambdaUpdate().eq(ActivityVerifier::getId, a.getId())
                        .set(ActivityVerifier::getEnabled, true).set(ActivityVerifier::getAssignedBy, actor)
                        .set(ActivityVerifier::getValidFrom, null).set(ActivityVerifier::getValidUntil, null));
        }
    }

    @Transactional
    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = users.selectByIdForUpdate(userId);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled()))
            throw new BusinessException(SystemResultCode.ACCOUNT_DISABLED);
        if (!encoder.matches(dto.getCurrentPassword(), user.getPasswordHash()))
            throw new BusinessException(SystemResultCode.PASSWORD_INVALID);
        if (dto.getCurrentPassword().equals(dto.getNewPassword()))
            throw new BusinessException(SystemResultCode.PASSWORD_UNCHANGED);
        tokens.revokeAll(userId, RevocationReason.LOGOUT);
        User patch = new User();
        patch.setId(userId);
        patch.setPasswordHash(encoder.encode(dto.getNewPassword()));
        patch.setMustChangePassword(false);
        if (users.updateById(patch) != 1) throw new BusinessException(SystemResultCode.CONFLICT);
    }
}

