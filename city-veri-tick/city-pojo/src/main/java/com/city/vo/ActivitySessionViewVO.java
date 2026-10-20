package com.city.vo;

import com.city.entity.ActivitySession;
import com.city.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ActivitySessionViewVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long activityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private Integer soldCount;
    private SessionStatus status;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;

    public static ActivitySessionViewVO from(ActivitySession session) {
        return new ActivitySessionViewVO(session.getId(), session.getActivityId(), session.getStartTime(),
                session.getEndTime(), session.getCapacity(), session.getSoldCount(), session.getStatus(),
                session.getSaleStartTime(), session.getSaleEndTime());
    }
}
