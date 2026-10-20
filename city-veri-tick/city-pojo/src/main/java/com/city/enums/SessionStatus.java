package com.city.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "场次状态：DRAFT-草稿；ON_SALE-售卖中；CLOSED-已结束；CANCELLED-已取消")
public enum SessionStatus {
    DRAFT,
    ON_SALE,
    CLOSED,
    CANCELLED
}
