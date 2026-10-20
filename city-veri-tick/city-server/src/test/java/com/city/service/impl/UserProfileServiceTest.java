package com.city.service.impl;
import com.city.service.UserProfileService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.city.dto.UpdateProfileDTO;
import com.city.entity.FileObject;
import com.city.entity.User;
import com.city.mapper.FileObjectMapper;
import com.city.mapper.UserMapper;
import com.city.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {
    private final UserProfileService service = new UserProfileServiceImpl();
    private final UserMapper users = mock(UserMapper.class);
    private final FileObjectMapper files = mock(FileObjectMapper.class);
    private final FileStorageService storage = mock(FileStorageService.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "fileObjectMapper", files);
        ReflectionTestUtils.setField(service, "fileStorageService", storage);
        ReflectionTestUtils.setField(service, "passwordEncoder", new BCryptPasswordEncoder());
        User user = new User();
        user.setId(12L);
        user.setPhone("13800138000");
        user.setNickname("神秘的幽灵");
        when(users.selectById(12L)).thenReturn(user);
    }

    @Test
    void storesOnlyHashAndMaskedIdentity() {
        UpdateProfileDTO request = new UpdateProfileDTO();
        request.setRealName("测试用户");
        request.setIdCardNo("11010119900101001x");
        var profile = service.update(12L, request);
        User saved = users.selectById(12L);
        assertThat(profile.isRealNameVerified()).isTrue();
        assertThat(profile.getIdCardMasked()).isEqualTo("110***********001X");
        assertThat(saved.getIdCardHash()).doesNotContain(request.getIdCardNo());
        assertThat(new BCryptPasswordEncoder()
                .matches("11010119900101001X", saved.getIdCardHash())).isTrue();
    }

    @Test
    void rejectsPartialIdentityWithoutUpdatingDatabase() {
        UpdateProfileDTO request = new UpdateProfileDTO();
        request.setRealName("测试用户");
        assertThatThrownBy(() -> service.update(12L, request)).isInstanceOf(RuntimeException.class);
        verify(users, never()).updateById(any(User.class));
    }

    @Test
    void returnsRegisteredPhoneAndNickname() {
        var profile = service.get(12L);
        assertThat(profile.getPhone()).isEqualTo("13800138000");
        assertThat(profile.getNickname()).isEqualTo("神秘的幽灵");
    }

    @Test
    void updatesNicknameAndOwnAvatar() {
        FileObject image = new FileObject();
        image.setId(9L);
        image.setUploadedBy(12L);
        image.setContentType("image/png");
        when(files.selectById(9L)).thenReturn(image);
        when(storage.getUrl(9L)).thenReturn("https://example.com/avatar.png");
        UpdateProfileDTO request = new UpdateProfileDTO();
        request.setNickname("自在的旅人");
        request.setAvatarFileId(9L);

        var profile = service.update(12L, request);

        assertThat(profile.getNickname()).isEqualTo("自在的旅人");
        assertThat(profile.getAvatarFileId()).isEqualTo(9L);
        assertThat(profile.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        verify(users).updateById(any(User.class));
    }

    @Test
    void rejectsAnotherUsersImage() {
        FileObject image = new FileObject();
        image.setId(9L);
        image.setUploadedBy(13L);
        image.setContentType("image/png");
        when(files.selectById(9L)).thenReturn(image);
        UpdateProfileDTO request = new UpdateProfileDTO();
        request.setAvatarFileId(9L);

        assertThatThrownBy(() -> service.update(12L, request)).isInstanceOf(RuntimeException.class);
        verify(users, never()).updateById(any(User.class));
    }
}
