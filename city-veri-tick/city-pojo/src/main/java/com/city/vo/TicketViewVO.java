package com.city.vo;
import com.city.json.ShanghaiOffsetDateTimeSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Schema(description = "门票视图")
@Data
@AllArgsConstructor
public class TicketViewVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long ticketId;
    private String verificationCode;
    private String status;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime checkedInAt;
    private Long verifierId;
    private String verifierName;
    private String verifierUsername;

    public TicketViewVO(Long ticketId, String verificationCode, String status) {
        this.ticketId=ticketId;this.verificationCode=verificationCode;this.status=status;
    }
}
