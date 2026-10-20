package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.city.enums.SessionStatus;

import java.time.LocalDateTime;
import java.io.Serializable;

@TableName("activity_session")
@Data
public class ActivitySession implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    private Long activityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private Integer capacity;
    private Integer soldCount;
    private SessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
