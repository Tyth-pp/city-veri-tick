package com.city.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.io.Serializable;

@TableName("check_in_record")
@Data
public class CheckInRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    private Long ticketId;
    private Long verifierId;
    private LocalDateTime checkedInAt;
    private String deviceCode;


}
