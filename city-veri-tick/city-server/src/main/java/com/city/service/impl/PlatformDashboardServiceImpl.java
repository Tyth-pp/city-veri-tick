package com.city.service.impl;
import java.util.Objects;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.entity.Activity;
import com.city.entity.CheckInRecord;
import com.city.entity.Organizer;
import com.city.entity.Ticket;
import com.city.entity.TicketOrder;
import com.city.mapper.ActivityMapper;
import com.city.mapper.CheckInRecordMapper;
import com.city.mapper.OrganizerMapper;
import com.city.mapper.TicketMapper;
import com.city.mapper.TicketOrderMapper;
import com.city.service.PlatformDashboardService;
import com.city.vo.PlatformDashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PlatformDashboardServiceImpl implements PlatformDashboardService {
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private OrganizerMapper organizerMapper;
    @Autowired
    private TicketOrderMapper ticketOrderMapper;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private CheckInRecordMapper checkInRecordMapper;

    @Override
    public PlatformDashboardVO summary() {
        List<Activity> activities = activityMapper.selectList(Wrappers.emptyWrapper());
        List<Organizer> organizers = organizerMapper.selectList(Wrappers.emptyWrapper());
        List<TicketOrder> orders = ticketOrderMapper.selectList(Wrappers.emptyWrapper());
        List<Ticket> tickets = ticketMapper.selectList(Wrappers.emptyWrapper());
        List<CheckInRecord> checkIns = checkInRecordMapper.selectList(Wrappers.emptyWrapper());

        PlatformDashboardVO vo = new PlatformDashboardVO();
        vo.setTotalActivities(activities.size());
        vo.setPendingActivities(activities.stream().filter(item -> "PENDING".equals(item.getStatus().name())).count());
        vo.setPublishedActivities(activities.stream().filter(item -> "PUBLISHED".equals(item.getStatus().name())).count());
        vo.setTotalOrganizers(organizers.size());
        vo.setApprovedOrganizers(organizers.stream().filter(item -> "APPROVED".equalsIgnoreCase(item.getStatus())).count());
        vo.setTotalOrders(orders.size());
        List<TicketOrder> paidOrders = orders.stream().filter(item -> "PAID".equalsIgnoreCase(item.getStatus())).toList();
        vo.setPaidOrders(paidOrders.size());
        vo.setPaidRevenue(paidOrders.stream().map(TicketOrder::getPayAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setTotalTickets(tickets.size());
        long checkedIn = tickets.stream().filter(item -> item.getStatus() != null && "CHECKED_IN".equals(item.getStatus().name())).count();
        vo.setCheckedInTickets(checkedIn);
        vo.setVerificationRate(tickets.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(checkedIn * 100.0 / tickets.size()).setScale(2, RoundingMode.HALF_UP));
        return vo;
    }
}
