package com.city.vo;

import com.city.entity.ActivitySession;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.city.json.ShanghaiOffsetDateTimeSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class UserActivitySessionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime startTime;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime endTime;
    private Integer capacity;
    private Integer availableCapacity;
    private List<UserTicketTypeVO> ticketTypes;

    public static UserActivitySessionVO from(ActivitySession session, List<UserTicketTypeVO> ticketTypes) {
        int soldCount = session.getSoldCount() == null ? 0 : session.getSoldCount();
        return new UserActivitySessionVO(session.getId(), session.getStartTime(), session.getEndTime(),
                session.getCapacity(), Math.max(0, session.getCapacity() - soldCount), ticketTypes);
    }
}
