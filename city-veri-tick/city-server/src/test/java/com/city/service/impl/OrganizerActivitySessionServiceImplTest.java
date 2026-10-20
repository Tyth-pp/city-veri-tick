package com.city.service.impl;

import com.city.dto.UpdateActivitySessionDTO;
import com.city.entity.Activity;
import com.city.entity.ActivitySession;
import com.city.entity.TicketType;
import com.city.enums.ActivityStatus;
import com.city.enums.SessionStatus;
import com.city.mapper.*;
import com.city.service.OrganizerAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrganizerActivitySessionServiceImplTest {
    private final OrganizerActivitySessionServiceImpl service = new OrganizerActivitySessionServiceImpl();
    private final ActivitySessionMapper sessions = mock(ActivitySessionMapper.class);
    private final ActivityMapper activities = mock(ActivityMapper.class);
    private final TicketTypeMapper types = mock(TicketTypeMapper.class);
    private final OrganizerAccessService access = mock(OrganizerAccessService.class);
    private final LocalDateTime start = LocalDateTime.of(2030, 1, 2, 18, 0);
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service, "activitySessionMapper", sessions);
        ReflectionTestUtils.setField(service, "activityMapper", activities);
        ReflectionTestUtils.setField(service, "organizerAccessService", access);
        Activity activity = new Activity(); activity.setOrganizerId(10L); activity.setStatus(ActivityStatus.DRAFT);
        ActivitySession session = new ActivitySession(); session.setId(2L); session.setActivityId(1L);
        session.setStatus(SessionStatus.DRAFT); session.setSoldCount(0);
        when(access.requireOrganizerId(7L)).thenReturn(10L);
        when(activities.selectById(1L)).thenReturn(activity); when(sessions.selectById(2L)).thenReturn(session);
    }
    private UpdateActivitySessionDTO dto() {
        UpdateActivitySessionDTO dto = new UpdateActivitySessionDTO();
        dto.setStartTime(start); dto.setEndTime(start.plusHours(2)); dto.setCapacity(100); return dto;
    }
    @Test void invalidSessionRangeHasActionableMessage() {
        UpdateActivitySessionDTO dto = dto(); dto.setEndTime(start);
        assertThatThrownBy(() -> service.update(7L, 2L, dto)).hasMessage("场次结束时间必须晚于开始时间");
        verify(sessions, never()).updateById(any(ActivitySession.class));
    }
    @Test void updatingSessionSalesWindowSynchronizesEveryTicketType() {
        ReflectionTestUtils.setField(service, "ticketTypeMapper", types);
        TicketType first = new TicketType(); first.setStock(30); TicketType second = new TicketType(); second.setStock(50);
        when(types.selectList(any())).thenReturn(List.of(first, second));
        UpdateActivitySessionDTO dto = dto();
        ReflectionTestUtils.setField(dto, "saleStartTime", start.minusDays(7));
        ReflectionTestUtils.setField(dto, "saleEndTime", start.minusHours(1));
        service.update(7L, 2L, dto);
        verify(types).updateById(first); verify(types).updateById(second);
        assertThat(first.getSaleStartTime()).isEqualTo(start.minusDays(7));
        assertThat(second.getSaleEndTime()).isEqualTo(start.minusHours(1));
    }
    @Test void rejectsSalesEndingAfterSessionStarts() {
        UpdateActivitySessionDTO dto = dto();
        ReflectionTestUtils.setField(dto, "saleStartTime", start.minusDays(7));
        ReflectionTestUtils.setField(dto, "saleEndTime", start.plusHours(1));
        assertThatThrownBy(() -> service.update(7L, 2L, dto)).hasMessage("售票结束时间不能晚于场次开始时间");
    }
}
