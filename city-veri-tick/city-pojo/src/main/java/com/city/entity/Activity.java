package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.city.enums.ActivityStatus;
import com.city.enums.ActivityCategory;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("activity")
@Data
public class Activity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    private Long organizerId;
    private String title;
    private ActivityCategory category;
    private String description;
    private String content;
    private String highlights;
    private String notices;
    private Long coverFileId;
    private String venueName;
    private String venueAddress;
    private ActivityStatus status;
    private Long createdBy;
    private Long submittedBy;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String rejectionReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
}
