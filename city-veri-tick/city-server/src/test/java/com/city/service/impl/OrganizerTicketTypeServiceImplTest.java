package com.city.service.impl;

import com.city.result.SystemResultCode;
import com.city.dto.CreateTicketTypeDTO;
import com.city.entity.Activity;
import com.city.entity.ActivitySession;
import com.city.entity.TicketType;
import com.city.enums.ActivityStatus;
import com.city.enums.SessionStatus;
import com.city.exception.BusinessException;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivitySessionMapper;
import com.city.mapper.TicketOrderMapper;
import com.city.mapper.TicketTypeMapper;
import com.city.service.OrganizerAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrganizerTicketTypeServiceImplTest {
    private final TicketTypeMapper ticketTypeMapper = mock(TicketTypeMapper.class);
    private final TicketOrderMapper ticketOrderMapper = mock(TicketOrderMapper.class);
    private final ActivitySessionMapper sessionMapper = mock(ActivitySessionMapper.class);
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final OrganizerAccessService accessService = mock(OrganizerAccessService.class);
    private final OrganizerTicketTypeServiceImpl service = new OrganizerTicketTypeServiceImpl();

    @BeforeEach
    void setUp() {
        reset(ticketTypeMapper, ticketOrderMapper, sessionMapper, activityMapper, accessService);
        ReflectionTestUtils.setField(service, "ticketTypeMapper", ticketTypeMapper);
        ReflectionTestUtils.setField(service, "ticketOrderMapper", ticketOrderMapper);
        ReflectionTestUtils.setField(service, "activitySessionMapper", sessionMapper);
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "organizerAccessService", accessService);
        when(accessService.requireOrganizerId(7L)).thenReturn(20L);
        when(sessionMapper.selectById(2L)).thenReturn(session(2L, 1L, 100));
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 20L));
    }

    @Test
    void createsTicketTypeForDraftSession() {
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of());
        CreateTicketTypeDTO dto = dto(60);

        service.create(7L, 2L, dto);

        ArgumentCaptor<TicketType> captor = ArgumentCaptor.forClass(TicketType.class);
        verify(ticketTypeMapper).insert(captor.capture());
        assertThat(captor.getValue().getLockedStock()).isZero();
        assertThat(captor.getValue().getSessionId()).isEqualTo(2L);
    }

    @Test
    void inheritsSalesWindowFromSessionInsteadOfRequiringRepeatedTicketDates() {
        ActivitySession session = session(2L, 1L, 100);
        ReflectionTestUtils.setField(session, "saleStartTime", LocalDateTime.of(2029, 12, 1, 0, 0));
        ReflectionTestUtils.setField(session, "saleEndTime", LocalDateTime.of(2030, 1, 1, 9, 0));
        when(sessionMapper.selectById(2L)).thenReturn(session);
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of());
        CreateTicketTypeDTO dto = dto(60);
        dto.setSaleStartTime(null);
        dto.setSaleEndTime(null);
        service.create(7L, 2L, dto);
        ArgumentCaptor<TicketType> captor = ArgumentCaptor.forClass(TicketType.class);
        verify(ticketTypeMapper).insert(captor.capture());
        assertThat(captor.getValue().getSaleStartTime()).isEqualTo(LocalDateTime.of(2029, 12, 1, 0, 0));
        assertThat(captor.getValue().getSaleEndTime()).isEqualTo(LocalDateTime.of(2030, 1, 1, 9, 0));
    }

    @Test
    void explainsInvalidSalesTime() {
        CreateTicketTypeDTO dto = dto(60);
        dto.setSaleEndTime(dto.getSaleStartTime());
        assertThatThrownBy(() -> service.create(7L, 2L, dto))
                .hasMessage("售票结束时间必须晚于售票开始时间");
        verify(ticketTypeMapper, never()).insert(any(TicketType.class));
    }

    @Test
    void rejectsStockBeyondSessionCapacity() {
        TicketType existing = new TicketType();
        existing.setName("VIP票");
        existing.setStock(50);
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(7L, 2L, dto(60)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode())
                                .isEqualTo(SystemResultCode.SESSION_CAPACITY_EXCEEDED));
    }

    private CreateTicketTypeDTO dto(int stock) {
        CreateTicketTypeDTO dto = new CreateTicketTypeDTO();
        dto.setName("普通票");
        dto.setPrice(new BigDecimal("99.00"));
        dto.setStock(stock);
        dto.setSaleStartTime(LocalDateTime.of(2029, 12, 1, 0, 0));
        dto.setSaleEndTime(LocalDateTime.of(2030, 1, 1, 9, 0));
        dto.setEnabled(true);
        return dto;
    }

    private ActivitySession session(Long id, Long activityId, int capacity) {
        ActivitySession session = new ActivitySession();
        session.setId(id);
        session.setActivityId(activityId);
        session.setCapacity(capacity);
        session.setStatus(SessionStatus.DRAFT);
        session.setStartTime(LocalDateTime.of(2030, 1, 1, 10, 0));
        return session;
    }

    private Activity activity(Long id, Long organizerId) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setOrganizerId(organizerId);
        activity.setStatus(ActivityStatus.DRAFT);
        return activity;
    }
}
