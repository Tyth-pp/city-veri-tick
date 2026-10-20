package com.city.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "活动分类")
public enum ActivityCategory {
    MOVIE,
    CONCERT,
    PERFORMANCE,
    STAND_UP,
    EXHIBITION_ART,
    MUSIC_FESTIVAL,
    OPERA,
    SPORTS,
    THEATRE_OPERA
}
