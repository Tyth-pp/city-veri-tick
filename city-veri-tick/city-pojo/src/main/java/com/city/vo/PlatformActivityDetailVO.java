package com.city.vo;

import com.city.entity.Activity;
import com.city.entity.Organizer;
import com.city.entity.User;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.city.json.ShanghaiOffsetDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class PlatformActivityDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private ActivityViewVO activity;
    private List<ActivitySessionViewVO> sessions;
    private List<PlatformTicketTypeVO> ticketTypes;
    private OrganizerInfo organizer;
    private Long submittedBy;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime submittedAt;
    private AccountInfo submitter;
    private AccountInfo reviewer;

    public record AccountInfo(Long id, String name, String username, String phone) implements Serializable {
        public static AccountInfo from(User user) {
            return user == null ? null : new AccountInfo(user.getId(), user.getStaffName(), user.getUsername(), user.getPhone());
        }
    }

    public record OrganizerInfo(Long id, String name, String status, String contactName, String contactPhone) implements Serializable {
        public static OrganizerInfo from(Organizer organizer) {
            return organizer == null ? null : new OrganizerInfo(organizer.getId(), organizer.getName(), organizer.getStatus(), organizer.getContactName(), organizer.getContactPhone());
        }
    }

    public static PlatformActivityDetailVO of(Activity activity, String coverFileUrl,
                                               List<ActivitySessionViewVO> sessions,
                                               List<PlatformTicketTypeVO> ticketTypes) {
        return new PlatformActivityDetailVO(ActivityViewVO.from(activity, coverFileUrl), sessions, ticketTypes,
                null, activity.getSubmittedBy(), activity.getSubmittedAt(), null, null);
    }
}
