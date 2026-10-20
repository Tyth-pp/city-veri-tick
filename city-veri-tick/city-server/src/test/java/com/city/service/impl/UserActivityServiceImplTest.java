package com.city.service.impl;
import com.city.cache.ActivityListCache;
import com.city.cache.PublicActivityCache;
import com.city.service.FileStorageService;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.city.result.SystemResultCode;
import com.city.entity.Activity;
import com.city.entity.ActivitySession;
import com.city.entity.TicketType;
import com.city.enums.ActivityStatus;
import com.city.enums.SessionStatus;
import com.city.exception.BusinessException;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivitySessionMapper;
import com.city.mapper.TicketTypeMapper;
import com.city.vo.UserActivityDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class UserActivityServiceImplTest {
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final ActivitySessionMapper activitySessionMapper = mock(ActivitySessionMapper.class);
    private final TicketTypeMapper ticketTypeMapper = mock(TicketTypeMapper.class);
    private final UserActivityServiceImpl service = new UserActivityServiceImpl();
    private final ActivityListCache listCache = mock(ActivityListCache.class);

    @BeforeEach
    void setUp() {
        PublicActivityCache cache=mock(PublicActivityCache.class);
        when(cache.getPublished(any())).thenAnswer(call->activityMapper.selectById((Long)call.getArgument(0)));
        ReflectionTestUtils.setField(service,"publicActivityCache",cache);
        reset(listCache);
        when(listCache.getPublished(org.mockito.ArgumentMatchers.nullable(String.class),org.mockito.ArgumentMatchers.nullable(String.class),any())).thenAnswer(call->((java.util.function.Supplier<List<Activity>>)call.getArgument(2)).get());
        ReflectionTestUtils.setField(service,"activityListCache",listCache);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), ActivitySession.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), TicketType.class);
        reset(activityMapper, activitySessionMapper, ticketTypeMapper);
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "activitySessionMapper", activitySessionMapper);
        ReflectionTestUtils.setField(service, "ticketTypeMapper", ticketTypeMapper);
    }

    @Test
    void listsPublishedActivitiesMatchingKeyword() {
        Activity activity = activity(1L, ActivityStatus.PUBLISHED);
        activity.setTitle("秋日音乐会");
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(session(2L, SessionStatus.ON_SALE)));
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of(ticketType(3L, true)));

        var result = service.list("音乐");

        assertThat(result).singleElement().extracting("id", "title")
                .containsExactly(1L, "秋日音乐会");
    }

    @Test
    void listsMinimumPriceAcrossVisibleSessionsAndHidesUnpricedActivities() {
        when(activityMapper.selectList(any())).thenReturn(List.of(
                activity(1L, ActivityStatus.PUBLISHED), activity(4L, ActivityStatus.PUBLISHED)));
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(
                session(2L, SessionStatus.ON_SALE), session(5L, SessionStatus.ON_SALE)));
        TicketType standard = ticketType(3L, true);
        TicketType cheapest = ticketType(6L, true);
        cheapest.setSessionId(5L);
        cheapest.setPrice(new BigDecimal("49.50"));
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of(standard, cheapest));

        var result = service.list(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getMinPrice()).isEqualByComparingTo("49.50");
        cheapest.setPrice(BigDecimal.ZERO);
        assertThat(service.list(null).get(0).getMinPrice()).isEqualByComparingTo("0");
    }
    @Test
    void returnsPublishedActivityWithOnSaleSessionsAndEnabledTicketTypes() {
        Activity activity = activity(1L, ActivityStatus.PUBLISHED);
        ActivitySession session = session(2L, SessionStatus.ON_SALE);
        TicketType ticketType = ticketType(3L, true);
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(session));
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of(ticketType));

        UserActivityDetailVO result = service.getDetail(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSessions()).singleElement().satisfies(sessionVO -> {
            assertThat(sessionVO.getId()).isEqualTo(2L);
            assertThat(sessionVO.getTicketTypes()).singleElement()
                    .extracting("id", "name", "enabled")
                    .containsExactly(3L, "普通票", true);
        });
    }

    @Test
    void hidesActivityThatIsNotPublished() {
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, ActivityStatus.DRAFT));

        assertThatThrownBy(() -> service.getDetail(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.NOT_FOUND));
    }

    @Test
    void excludesEndedSessionsEvenWhenTicketSalesAreStillOpen() {
        ActivitySession ended = session(2L, SessionStatus.ON_SALE);
        ended.setEndTime(LocalDateTime.now().minusHours(1));
        ended.setStartTime(LocalDateTime.now().minusHours(3));
        when(activityMapper.selectList(any())).thenReturn(List.of(activity(1L, ActivityStatus.PUBLISHED)));
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, ActivityStatus.PUBLISHED));
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(ended));
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of(ticketType(3L, true)));
        assertThat(service.list(null)).isEmpty();
        assertThatThrownBy(() -> service.getDetail(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void keepsFutureSessionWhenAnotherSessionHasEnded() {
        ActivitySession ended = session(2L, SessionStatus.ON_SALE);
        ended.setEndTime(LocalDateTime.now().minusHours(1));
        ActivitySession future = session(5L, SessionStatus.ON_SALE);
        TicketType futureTicket = ticketType(6L, true);
        futureTicket.setSessionId(5L);
        when(activityMapper.selectList(any())).thenReturn(List.of(activity(1L, ActivityStatus.PUBLISHED)));
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(ended, future));
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of(ticketType(3L, true), futureTicket));
        assertThat(service.list(null)).singleElement().extracting("startTime").isEqualTo(future.getStartTime());
    }

    @Test
    void renewsCoverForEndedPublishedActivityWithoutMakingItSaleable() {
        Activity activity = activity(1L, ActivityStatus.PUBLISHED);
        activity.setCoverFileId(20L);
        ActivitySession ended = session(2L, SessionStatus.ON_SALE);
        ended.setEndTime(LocalDateTime.now().minusHours(1));
        FileStorageService storage = mock(FileStorageService.class);
        ReflectionTestUtils.setField(service, "fileStorageService", storage);
        when(storage.getUrl(20L)).thenReturn("fresh-signed-cover");
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(ended));
        var result = service.getPreview(1L);
        assertThat(result.availability()).isEqualTo("ENDED");
        assertThat(result.activity().getCoverFileUrl()).isEqualTo("fresh-signed-cover");
    }

    @Test
    void exposesUpcomingActivityInListDetailAndPreviewWithOpeningTime() {
        TicketType upcoming = ticketType(3L, true);
        upcoming.setSaleStartTime(LocalDateTime.now().plusHours(2));
        upcoming.setSaleEndTime(LocalDateTime.now().plusHours(4));
        when(activityMapper.selectList(any())).thenReturn(List.of(activity(1L, ActivityStatus.PUBLISHED)));
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, ActivityStatus.PUBLISHED));
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(session(2L, SessionStatus.ON_SALE)));
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of(upcoming));
        var list = service.list(null);
        assertThat(list).singleElement().extracting("availability", "saleStartTime")
                .containsExactly("UPCOMING", upcoming.getSaleStartTime());
        assertThat(service.getDetail(1L).getSessions().get(0).getTicketTypes()).hasSize(1);
        assertThat(service.getPreview(1L).availability()).isEqualTo("UPCOMING");
    }

    @Test
    void hidesExpiredSalesAndDisabledTicketsEvenWithFutureSessions() {
        TicketType expired = ticketType(3L, true);
        expired.setSaleEndTime(LocalDateTime.now().minusMinutes(1));
        TicketType disabled = ticketType(4L, false);
        disabled.setSaleStartTime(LocalDateTime.now().plusHours(1));
        when(activityMapper.selectList(any())).thenReturn(List.of(activity(1L, ActivityStatus.PUBLISHED)));
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, ActivityStatus.PUBLISHED));
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(session(2L, SessionStatus.ON_SALE)));
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of(expired, disabled));
        assertThat(service.list(null)).isEmpty();
        assertThatThrownBy(() -> service.getDetail(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void previewDoesNotExposeDraftOrOfflineActivity() {
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, ActivityStatus.DRAFT));
        assertThatThrownBy(() -> service.getPreview(1L)).isInstanceOf(BusinessException.class);
        when(activityMapper.selectById(1L)).thenReturn(activity(1L, ActivityStatus.OFFLINE));
        assertThatThrownBy(() -> service.getPreview(1L)).isInstanceOf(BusinessException.class);
    }

    private Activity activity(Long id, ActivityStatus status) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setOrganizerId(10L);
        activity.setTitle("测试活动");
        activity.setStatus(status);
        return activity;
    }

    private ActivitySession session(Long id, SessionStatus status) {
        ActivitySession session = new ActivitySession();
        session.setId(id);
        session.setActivityId(1L);
        LocalDateTime now = LocalDateTime.now();
        session.setStartTime(now.plusDays(1));
        session.setEndTime(now.plusDays(1).plusHours(2));
        session.setCapacity(100);
        session.setSoldCount(0);
        session.setStatus(status);
        return session;
    }

    private TicketType ticketType(Long id, boolean enabled) {
        TicketType type = new TicketType();
        type.setId(id);
        type.setSessionId(2L);
        type.setName("普通票");
        type.setPrice(new BigDecimal("99.00"));
        type.setStock(100);
        type.setLockedStock(0);
        LocalDateTime now = LocalDateTime.now();
        type.setSaleStartTime(now.minusDays(1));
        type.setSaleEndTime(now.plusDays(1));
        type.setEnabled(enabled);
        return type;
    }

    @Test
    void cachedMetadataStillUsesCurrentTicketStockAndSaleWindow() {
        PublicActivityCache cache=mock(PublicActivityCache.class);
        ReflectionTestUtils.setField(service,"publicActivityCache",cache);
        reset(listCache);
        when(listCache.getPublished(org.mockito.ArgumentMatchers.nullable(String.class),org.mockito.ArgumentMatchers.nullable(String.class),any())).thenAnswer(call->((java.util.function.Supplier<List<Activity>>)call.getArgument(2)).get());
        ReflectionTestUtils.setField(service,"activityListCache",listCache);
        when(cache.getPublished(1L)).thenReturn(activity(1L,ActivityStatus.PUBLISHED));
        when(activitySessionMapper.selectList(any())).thenReturn(List.of(session(2L,SessionStatus.ON_SALE)));
        TicketType type=ticketType(3L,true);type.setStock(5);
        when(ticketTypeMapper.selectList(any())).thenReturn(List.of(type));
        assertThat(service.getDetail(1L).getSessions().get(0).getTicketTypes().get(0).getAvailableStock()).isEqualTo(5);
        type.setStock(0);
        assertThat(service.getDetail(1L).getSessions().get(0).getTicketTypes().get(0).getAvailableStock()).isZero();
        type.setSaleEndTime(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(()->service.getDetail(1L)).isInstanceOf(BusinessException.class);
    }
}
