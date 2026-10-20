package com.city.service.impl;

import com.city.entity.Activity;
import com.city.mapper.ActivityMapper;
import com.city.mapper.UserMapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.result.SystemResultCode;
import com.city.entity.Organizer;
import com.city.exception.BusinessException;
import com.city.mapper.OrganizerMapper;
import com.city.result.PageResult;
import com.city.service.PlatformOrganizerService;
import com.city.vo.OrganizerAdminViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import com.city.utils.SearchTextUtils;

@Service
public class PlatformOrganizerServiceImpl implements PlatformOrganizerService {
    @Autowired
    private OrganizerMapper organizerMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ActivityMapper activityMapper;

    private OrganizerAdminViewVO view(Organizer org) {
        OrganizerAdminViewVO v = OrganizerAdminViewVO.from(org);
        v.setAdministratorCount(userMapper.countAdministrators(org.getId()));
        v.setActivityCount(activityMapper.selectCount(Wrappers.<Activity>lambdaQuery().eq(Activity::getOrganizerId, org.getId())));
        return v;
    }

    @Override
    public PageResult<OrganizerAdminViewVO> page(String keyword, String status, long page, long pageSize) {
        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        String normalizedKeyword = SearchTextUtils.normalizeKeyword(keyword);
        List<OrganizerAdminViewVO> filtered = organizerMapper.selectList(
                        Wrappers.<Organizer>lambdaQuery().orderByDesc(Organizer::getCreatedAt))
                .stream()
                .filter(item -> status == null || status.isBlank()
                        || item.getStatus().equalsIgnoreCase(status.trim()))
                .filter(item -> normalizedKeyword.isBlank()
                        || SearchTextUtils.containsKeyword(item.getName(), normalizedKeyword)
                        || SearchTextUtils.containsKeyword(item.getContactName(), normalizedKeyword)
                        || SearchTextUtils.containsKeyword(item.getContactPhone(), normalizedKeyword)
                        || SearchTextUtils.containsKeyword(item.getUnifiedSocialCreditCode(), normalizedKeyword)
                        || String.valueOf(item.getId()).contains(normalizedKeyword))
                .map(this::view)
                .toList();
        int from = (int) Math.min(filtered.size(), (safePage - 1) * safePageSize);
        int to = (int) Math.min(filtered.size(), from + safePageSize);
        return new PageResult<>(filtered.subList(from, to), filtered.size(), safePage, safePageSize);
    }

    @Override
    public OrganizerAdminViewVO detail(Long organizerId) {
        Organizer organizer = organizerMapper.selectById(organizerId);
        if (organizer == null) {
            throw new BusinessException(SystemResultCode.NOT_FOUND);
        }
        return view(organizer);
    }

}
