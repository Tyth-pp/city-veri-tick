package com.city.service.impl;
import com.city.cache.ActivityListCache;
import com.city.service.ManagementMessageService;
import com.city.cache.PublicActivityCache;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.city.dto.UpdateActivityDTO;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.city.result.SystemResultCode;
import com.city.dto.CreateActivityDTO;
import com.city.entity.Activity;
import com.city.entity.ActivitySession;
import com.city.enums.ActivityStatus;
import com.city.exception.BusinessException;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivitySessionMapper;
import com.city.mapper.TicketTypeMapper;
import com.city.service.OrganizerAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import java.util.List;

class OrganizerActivityServiceImplTest {
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final OrganizerAccessService accessService = mock(OrganizerAccessService.class);
    private final ActivitySessionMapper activitySessionMapper = mock(ActivitySessionMapper.class);
    private final TicketTypeMapper ticketTypeMapper = mock(TicketTypeMapper.class);
    private final OrganizerActivityServiceImpl service = new OrganizerActivityServiceImpl();

    private final PublicActivityCache publicCache=mock(PublicActivityCache.class);
    private final ActivityListCache listCache=mock(ActivityListCache.class);

    @BeforeEach
    void setUp() {
        reset(publicCache);
        ReflectionTestUtils.setField(service,"publicActivityCache",publicCache);
        ReflectionTestUtils.setField(service,"activityListCache",listCache);
        ReflectionTestUtils.setField(service,"messages",mock(ManagementMessageService.class));
        reset(activityMapper, activitySessionMapper, ticketTypeMapper, accessService);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Activity.class);
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "activitySessionMapper", activitySessionMapper);
        ReflectionTestUtils.setField(service, "ticketTypeMapper", ticketTypeMapper);
        ReflectionTestUtils.setField(service, "organizerAccessService", accessService);
    }

    @Test
    void createsDraftForAuthenticatedUsersOrganizer() {
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        CreateActivityDTO dto = new CreateActivityDTO();
        dto.setTitle("City concert");

        service.create(7L, dto);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(captor.capture());
        assertThat(captor.getValue().getOrganizerId()).isEqualTo(20L);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(7L);
        assertThat(captor.getValue().getStatus()).isEqualTo(ActivityStatus.DRAFT);
    }

    @Test
    void rejectsAccessToAnotherOrganizersActivity() {
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        Activity activity = activity(1L, 21L, ActivityStatus.DRAFT);
        when(activityMapper.selectById(1L)).thenReturn(activity);

        assertThatThrownBy(() -> service.submit(7L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.FORBIDDEN));
    }

    @Test
    void rejectsSubmittingPublishedActivity() {
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 20L, ActivityStatus.PUBLISHED));

        assertThatThrownBy(() -> service.submit(7L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.CONFLICT));
        verify(activityMapper, never()).update(isNull(), any(Wrapper.class));
    }

    @Test
    void rejectsSubmittingActivityWithoutSessions() {
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 20L, ActivityStatus.DRAFT));
        when(activitySessionMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.submit(7L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode())
                                .isEqualTo(SystemResultCode.ACTIVITY_SESSION_REQUIRED));
        verify(activityMapper, never()).update(isNull(), any(Wrapper.class));
    }

    @Test
    void submitsActivityWhenSessionExists() {
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 20L, ActivityStatus.DRAFT));
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(session(2L)));
        when(ticketTypeMapper.selectCount(any())).thenReturn(1L);
        when(activityMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        service.submit(7L, 1L);
        verify(publicCache).invalidateAfterCommit(1L);

        ArgumentCaptor<Wrapper<Activity>> update = ArgumentCaptor.forClass(Wrapper.class);
        verify(activityMapper).update(isNull(), update.capture());
        var wrapper = (LambdaUpdateWrapper<Activity>) update.getValue();
        assertThat(wrapper.getSqlSet()).contains("submitted_by", "submitted_at");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(7L);
        assertThat(wrapper.getParamNameValuePairs().values()).anyMatch(value -> value instanceof LocalDateTime);
    }

    @Test
    void rejectsSubmittingWhenSessionHasNoEnabledTicketType() {
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 20L, ActivityStatus.DRAFT));
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(session(2L)));
        when(ticketTypeMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.submit(7L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.TICKET_TYPE_REQUIRED));
    }

    @Test
    void deletesOwnedDraftActivity() {
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 20L, ActivityStatus.DRAFT));
        when(activityMapper.delete(any(Wrapper.class))).thenReturn(1);

        service.delete(7L, 1L);
        verify(publicCache).invalidateAfterCommit(1L);

        verify(activityMapper).delete(any(Wrapper.class));
    }

    @Test
    void rejectsDeletingAnotherOrganizersDraft() {
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 21L, ActivityStatus.DRAFT));

        assertThatThrownBy(() -> service.delete(7L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.FORBIDDEN));
        verify(activityMapper, never()).delete(any(Wrapper.class));
    }

    @Test
    void rejectsDeletingPublishedActivity() {
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 20L, ActivityStatus.PUBLISHED));

        assertThatThrownBy(() -> service.delete(7L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.CONFLICT));
        verify(activityMapper, never()).delete(any(Wrapper.class));
    }

    private Activity activity(Long id, Long organizerId, ActivityStatus status) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setOrganizerId(organizerId);
        activity.setStatus(status);
        return activity;
    }

    private ActivitySession session(Long id) {
        ActivitySession session = new ActivitySession();
        session.setId(id);
        session.setActivityId(1L);
        return session;
    }

    @Test void changedDraftInvalidatesAnyOldNegativeEntry() {
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        when(activityMapper.selectById(1L)).thenReturn(activity(1L,20L,ActivityStatus.DRAFT));
        when(activityMapper.update(isNull(),any(Wrapper.class))).thenReturn(1);
        var dto=new UpdateActivityDTO();dto.setTitle("更新后的活动");
        service.update(7L,1L,dto);verify(publicCache).invalidateAfterCommit(1L);
    }
}
