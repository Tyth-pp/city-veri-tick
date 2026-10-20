package com.city.entity;

import com.city.json.ShanghaiOffsetDateTimeSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account_operation")
public class AccountOperation {
    @TableId
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String operatorUsername;
    private Long targetUserId;
    private String operation;
    private String reason;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime createdAt;
}
