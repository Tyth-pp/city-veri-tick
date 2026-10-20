package com.city.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PlatformVerifierViewVO implements Serializable {
    private Long id;
    private Long activityId;
    private String activityTitle;
    private Long userId;
    private String username;
    private String nickname;
    private String name;
    private String phone;
    private Boolean enabled;
    private Long assignedBy;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime createdAt;
}
