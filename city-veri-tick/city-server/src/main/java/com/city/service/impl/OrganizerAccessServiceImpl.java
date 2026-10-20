package com.city.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.result.SystemResultCode;
import com.city.entity.OrganizerMember;
import com.city.exception.BusinessException;
import com.city.mapper.OrganizerMemberMapper;
import com.city.service.OrganizerAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrganizerAccessServiceImpl implements OrganizerAccessService {
    @Autowired
    private OrganizerMemberMapper organizerMemberMapper;

    @Override
    public Long requireOrganizerId(Long userId) {
        OrganizerMember member = organizerMemberMapper.selectOne(Wrappers.<OrganizerMember>lambdaQuery()
                .eq(OrganizerMember::getUserId, userId)
                .eq(OrganizerMember::getEnabled, true)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BusinessException(SystemResultCode.FORBIDDEN);
        }
        return member.getOrganizerId();
    }
}
