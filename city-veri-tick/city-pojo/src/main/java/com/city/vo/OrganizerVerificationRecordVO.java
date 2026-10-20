package com.city.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.city.json.ShanghaiOffsetDateTimeSerializer;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrganizerVerificationRecordVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long ticketId;
    private String ticketNo;
    private String verificationCode;
    private String ticketStatus;
    private Long orderId;
    private String orderNo;
    private String orderStatus;
    private Long activityId;
    private String activityTitle;
    private Long sessionId;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime sessionStartTime;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime sessionEndTime;
    private String coverFileUrl;
    private String ticketTypeName;
    private Long verifierId;
    private String verifierName;
    private String verifierUsername;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime checkedInAt;
}
