package com.city.service.impl;

import com.city.service.StaffProfileService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.result.SystemResultCode;
import com.city.dto.StaffProfileDTO;
import com.city.entity.*;
import com.city.exception.BusinessException;
import com.city.mapper.*;
import com.city.service.OrganizerAccessService;
import com.city.vo.StaffProfileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffProfileServiceImpl implements StaffProfileService {
    @Autowired
    private UserMapper users;
    @Autowired
    private OrganizerMemberMapper members;
    @Autowired
    private OrganizerAccessService access;

    private String staffRole(Long id) {
        for (String role : new String[]{"PLATFORM_ADMIN", "ORGANIZER_ADMIN", "VERIFIER"})
            if (users.countRole(id, role) > 0) return role;
        throw new BusinessException(SystemResultCode.FORBIDDEN);
    }

    @Override
    public StaffProfileVO self(Long id) {
        String role = staffRole(id);
        User u = users.selectById(id);
        if (u == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        StaffProfileVO v = new StaffProfileVO();
        v.setId(u.getId());
        v.setUsername(u.getUsername());
        v.setName(u.getStaffName());
        v.setPhone(u.getPhone());
        v.setRole(role);
        v.setEnabled(u.getEnabled());
        v.setOrganizers(users.selectOrganizations(id));
        v.setCreatedAt(u.getCreatedAt());
        return v;
    }

    @Transactional
    @Override
    public void updateSelf(Long actor, StaffProfileDTO dto) {
        staffRole(actor);
        update(actor, dto, true);
    }

    @Transactional
    @Override
    public void updateManaged(Long actor, Long target, StaffProfileDTO dto) {
        // Self-editing uses /account/profile. Platform accounts retain existing protection.
        if (actor.equals(target) || "PLATFORM_ADMIN".equals(staffRole(target)))
            throw new BusinessException(SystemResultCode.FORBIDDEN);
        update(target, dto, false);
    }

    @Transactional
    @Override
    public void updateVerifier(Long actor, Long target, StaffProfileDTO dto) {
        Long org = access.requireOrganizerId(actor);
        if (!"VERIFIER".equals(staffRole(target))) throw new BusinessException(SystemResultCode.FORBIDDEN);
        OrganizerMember member = members.selectOne(Wrappers.<OrganizerMember>lambdaQuery()
                .eq(OrganizerMember::getOrganizerId, org).eq(OrganizerMember::getUserId, target).eq(OrganizerMember::getEnabled, true));
        if (member == null) throw new BusinessException(SystemResultCode.FORBIDDEN);
        update(target, dto, false);
    }

    private void update(Long id, StaffProfileDTO dto, boolean self) {
        User u = users.selectByIdForUpdate(id);
        if (u == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        if (self && !Boolean.TRUE.equals(u.getEnabled()))
            throw new BusinessException(SystemResultCode.ACCOUNT_DISABLED);
        User occupied = users.selectByPhone(dto.getPhone());
        if (occupied != null && !id.equals(occupied.getId()))
            throw new BusinessException(SystemResultCode.ACCOUNT_DUPLICATE);
        User patch = new User();
        patch.setId(id);
        patch.setStaffName(dto.getName().trim());
        patch.setPhone(dto.getPhone());
        if (users.updateById(patch) != 1) throw new BusinessException(SystemResultCode.CONFLICT);
    }
}
