package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("activity_verifier")
public class ActivityVerifier implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    private Long activityId;
    private Long userId;
    private Long assignedBy;
    private Boolean enabled;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime createdAt;
}
