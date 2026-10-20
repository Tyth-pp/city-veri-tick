package com.city.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "活动状态：DRAFT-草稿；PENDING-待平台审核；PUBLISHED-已发布；"
        + "REJECTED-审核驳回；OFFLINE-已下架；CANCELLED-已取消")
public enum ActivityStatus {
    DRAFT,
    PENDING,
    PUBLISHED,
    REJECTED,
    OFFLINE,
    CANCELLED
}
