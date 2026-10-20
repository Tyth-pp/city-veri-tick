package com.city.vo;

import com.city.entity.Activity;
import com.city.enums.ActivityCategory;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.city.json.ShanghaiOffsetDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserActivitySummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private ActivityCategory category;
    private String description;
    private String content;
    private String highlights;
    private String notices;
    private Long coverFileId;
    private String coverFileUrl;
    private String venueName;
    private String venueAddress;
    private BigDecimal minPrice;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime startTime;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime endTime;
    private String availability;
    @JsonSerialize(using = ShanghaiOffsetDateTimeSerializer.class)
    private LocalDateTime saleStartTime;

    public static UserActivitySummaryVO from(Activity activity, String coverFileUrl) {
        return new UserActivitySummaryVO(activity.getId(), activity.getTitle(), activity.getCategory(), activity.getDescription(),
                activity.getContent(), activity.getHighlights(), activity.getNotices(), activity.getCoverFileId(), coverFileUrl, activity.getVenueName(), activity.getVenueAddress(), null, null, null, null, null);
    }

    public static UserActivitySummaryVO from(Activity activity) {
        return from(activity, null);
    }
}
