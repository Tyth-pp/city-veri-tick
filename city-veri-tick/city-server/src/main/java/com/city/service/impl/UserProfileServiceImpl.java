package com.city.service.impl;

import com.city.service.UserProfileService;

import java.util.Locale;

import com.city.result.SystemResultCode;
import com.city.dto.UpdateProfileDTO;
import com.city.entity.FileObject;
import com.city.entity.User;
import com.city.exception.BusinessException;
import com.city.mapper.FileObjectMapper;
import com.city.mapper.UserMapper;
import com.city.service.FileStorageService;
import com.city.vo.UserProfileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FileObjectMapper fileObjectMapper;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserProfileVO get(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        return toView(user);
    }

    @Transactional
    @Override
    public UserProfileVO update(Long userId, UpdateProfileDTO request) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        if (request.getNickname() != null) {
            String nickname = request.getNickname().trim();
            if (nickname.length() < 2 || nickname.length() > 24)
                throw new BusinessException(SystemResultCode.BAD_REQUEST);
            user.setNickname(nickname);
        }
        if (request.getAvatarFileId() != null) {
            FileObject file = fileObjectMapper.selectById(request.getAvatarFileId());
            if (file == null || !userId.equals(file.getUploadedBy()) || file.getContentType() == null
                    || !file.getContentType().startsWith("image/")) {
                throw new BusinessException(SystemResultCode.BAD_REQUEST);
            }
            user.setAvatarFileId(file.getId());
        }
        if (request.getRealName() != null || request.getIdCardNo() != null) {
            if (request.getRealName() == null || request.getIdCardNo() == null) {
                throw new BusinessException(SystemResultCode.BAD_REQUEST);
            }
            String name = request.getRealName().trim();
            String idCard = request.getIdCardNo().trim().toUpperCase(Locale.ROOT);
            if (name.length() < 2 || name.length() > 32 || !idCard.matches("\\d{17}[\\dX]")) {
                throw new BusinessException(SystemResultCode.BAD_REQUEST);
            }
            user.setRealName(name);
            user.setIdCardHash(passwordEncoder.encode(idCard));
            user.setIdCardMasked(idCard.substring(0, 3) + "***********" + idCard.substring(14));
        }
        userMapper.updateById(user);
        return toView(user);
    }

    private UserProfileVO toView(User user) {
        Long avatarFileId = user.getAvatarFileId();
        String idCard = user.getIdCardHash();
        return new UserProfileVO(user.getNickname(), user.getPhone(), avatarFileId,
                avatarFileId == null ? null : fileStorageService.getUrl(avatarFileId), user.getRealName(),
                user.getIdCardMasked(), idCard != null && user.getRealName() != null);
    }
}
