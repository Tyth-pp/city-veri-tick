package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("organizer_member")
public class OrganizerMember implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    private Long organizerId;
    private Long userId;
    private String memberRole;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
