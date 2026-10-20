package com.city.vo;

import com.city.json.ShanghaiOffsetDateTimeSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class VerifyTicketViewVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long ticketId;
    private String ticketNo;
    private String verificationCode;
    private String ticketStatus;
    private String orderStatus;
    private Long activityId;
    private String activityTitle;
    private Long sessionId;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime sessionStartTime;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime sessionEndTime;
    private String ticketTypeName;
    private String realName;
    private String idCardMasked;
    private String message;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime checkedInAt;
}
