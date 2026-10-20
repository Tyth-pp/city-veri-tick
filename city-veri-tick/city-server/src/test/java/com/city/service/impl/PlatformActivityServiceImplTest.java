package com.city.service.impl;
import com.city.cache.ActivityListCache;
import com.city.service.ManagementMessageService;
import com.city.cache.PublicActivityCache;
import com.city.mapper.UserMapper;
import com.city.entity.Organizer;
import com.city.entity.User;
import com.city.mapper.OrganizerMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.city.result.SystemResultCode;
import com.city.entity.Activity;
import com.city.enums.ActivityStatus;
import com.city.exception.BusinessException;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivitySessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class PlatformActivityServiceImplTest {
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final ActivitySessionMapper activitySessionMapper = mock(ActivitySessionMapper.class);
    private final PlatformActivityServiceImpl service = new PlatformActivityServiceImpl();

    private final PublicActivityCache publicCache=mock(PublicActivityCache.class);
    private final ActivityListCache listCache=mock(ActivityListCache.class);

    @BeforeEach
    void setUp() {
        reset(publicCache);
        ReflectionTestUtils.setField(service,"publicActivityCache",publicCache);
        ReflectionTestUtils.setField(service,"activityListCache",listCache);
        ReflectionTestUtils.setField(service,"messages",mock(ManagementMessageService.class));
        reset(activityMapper, activitySessionMapper);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Activity.class);
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "activitySessionMapper", activitySessionMapper);
    }

    @Test
    void detailIncludesOnlySafeOrganizerAndActualSubmissionAccounts() {
        var organizerMapper = mock(OrganizerMapper.class);
        var userMapper = mock(UserMapper.class);
        ReflectionTestUtils.setField(service, "organizerMapper", organizerMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        Activity activity = activity(ActivityStatus.PENDING);
        activity.setOrganizerId(20L);
        ReflectionTestUtils.setField(activity, "submittedBy", 7L);
        ReflectionTestUtils.setField(activity, "submittedAt", LocalDateTime.of(2026,10,15,10,0));
        activity.setCreatedBy(3L);
        activity.setReviewedBy(9L);
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(activitySessionMapper.selectList(any())).thenReturn(List.of());
        var organizer = new Organizer(); organizer.setId(20L); organizer.setName("City culture"); organizer.setContactName("Contact Li");
        when(organizerMapper.selectById(20L)).thenReturn(organizer);
        var user = new User(); user.setId(7L); user.setStaffName("Submitter Zhang"); user.setUsername("zhang"); user.setPasswordHash("secret-hash"); user.setIdCardHash("secret-id");
        when(userMapper.selectById(7L)).thenReturn(user);
        var reviewer = new User();reviewer.setId(9L);reviewer.setStaffName("Reviewer Li");
        when(userMapper.selectById(9L)).thenReturn(reviewer);
        var json = new ObjectMapper().findAndRegisterModules().valueToTree(service.detail(1L));
        assertThat(json.path("organizer").path("contactName").asText()).isEqualTo("Contact Li");
        assertThat(json.path("submitter").path("name").asText()).isEqualTo("Submitter Zhang");
        assertThat(json.path("reviewer").path("name").asText()).isEqualTo("Reviewer Li");
        assertThat(json.path("submittedAt").isNull()).isFalse();
        assertThat(json.toString()).doesNotContain("secret-hash", "secret-id", "passwordHash", "idCardHash");
        verify(userMapper, never()).selectById(3L);
    }

    @Test
    void historicalDetailDoesNotInventSubmissionFromCreator() {
        var organizerMapper = mock(OrganizerMapper.class);
        var userMapper = mock(UserMapper.class);
        ReflectionTestUtils.setField(service, "organizerMapper", organizerMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        var activity = activity(ActivityStatus.PUBLISHED); activity.setCreatedBy(3L);
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(activitySessionMapper.selectList(any())).thenReturn(List.of());
        var json = new ObjectMapper().findAndRegisterModules().valueToTree(service.detail(1L));
        assertThat(json.path("submitter").isNull()).isTrue();
        assertThat(json.path("submittedAt").isNull()).isTrue();
        verifyNoInteractions(userMapper);
    }

    @Test
    void approvesPendingActivityWithConditionalUpdate() {
        when(activityMapper.selectById(1L)).thenReturn(activity(ActivityStatus.PENDING));
        when(activityMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        service.approve(9L, 1L);
        verify(publicCache).invalidateAfterCommit(1L);

        verify(activityMapper).update(isNull(), any(Wrapper.class));
        verify(activitySessionMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void reportsConflictWhenStatusChangedConcurrently() {
        when(activityMapper.selectById(1L)).thenReturn(activity(ActivityStatus.PENDING));
        when(activityMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> service.approve(9L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.CONFLICT));
    }

    @Test
    void reportsMissingActivity() {
        when(activityMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.reject(9L, 1L, "Incomplete details"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.NOT_FOUND));
    }

    private Activity activity(ActivityStatus status) {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus(status);
        return activity;
    }

    @Test void rejectionAndOfflineBothInvalidateStaticMetadata() {
        when(activityMapper.selectById(1L)).thenReturn(activity(ActivityStatus.PENDING));
        when(activityMapper.update(isNull(),any(Wrapper.class))).thenReturn(1);
        service.reject(9L,1L,"资料不完整");verify(publicCache).invalidateAfterCommit(1L);
        reset(publicCache);
        when(activityMapper.selectById(1L)).thenReturn(activity(ActivityStatus.PUBLISHED));
        service.offline(9L,1L,"活动取消");verify(publicCache).invalidateAfterCommit(1L);
    }
    @Test void failedReviewNeverRegistersInvalidation() {
        when(activityMapper.selectById(1L)).thenReturn(activity(ActivityStatus.PENDING));
        when(activityMapper.update(isNull(),any(Wrapper.class))).thenReturn(0);
        assertThatThrownBy(()->service.approve(9L,1L)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(publicCache);
    }
}
