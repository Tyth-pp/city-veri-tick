package com.city.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.entity.*;
import com.city.mapper.*;
import com.city.vo.*;


public final class ManagementOrderSupport {
    private ManagementOrderSupport() {
    }

    public static void identity(OrderViewVO view, TicketOrder order, Activity activity, OrganizerMapper organizers) {
        view.setRealName(order.getRealName());
        view.setIdCardMasked(order.getIdCardMasked());
        if (activity == null) return;
        view.setVenueName(activity.getVenueName());
        view.setOrganizerId(activity.getOrganizerId());
        if (activity.getOrganizerId() != null) {
            Organizer organizer = organizers.selectById(activity.getOrganizerId());
            if (organizer != null) view.setOrganizerName(organizer.getName());
        }
    }

    public static TicketViewVO ticket(Ticket ticket, CheckInRecordMapper records, UserMapper users) {
        TicketViewVO view = new TicketViewVO(ticket.getId(), ticket.getVerificationCode(), ticket.getStatus().name());
        CheckInRecord record = records.selectOne(Wrappers.<CheckInRecord>lambdaQuery().eq(CheckInRecord::getTicketId, ticket.getId()).orderByDesc(CheckInRecord::getCheckedInAt).last("LIMIT 1"));
        if (record != null) {
            view.setCheckedInAt(record.getCheckedInAt());
            view.setVerifierId(record.getVerifierId());
            if (record.getVerifierId() != null) {
                User operator = users.selectById(record.getVerifierId());
                if (operator != null) {
                    view.setVerifierName(operator.getStaffName());
                    view.setVerifierUsername(operator.getUsername());
                }
            }
        }
        return view;
    }
}
