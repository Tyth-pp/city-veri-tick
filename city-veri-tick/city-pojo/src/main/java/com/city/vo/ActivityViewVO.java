package com.city.vo;

import com.city.entity.Activity;
import com.city.enums.ActivityStatus;
import com.city.enums.ActivityCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ActivityViewVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long organizerId;
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
    private ActivityStatus status;
    private String rejectionReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private Long submittedBy;
    private LocalDateTime submittedAt;
    private String organizerName;
    private String submitterName;

    public static ActivityViewVO from(Activity activity, String coverFileUrl) {
        return new ActivityViewVO(activity.getId(), activity.getOrganizerId(), activity.getTitle(), activity.getCategory(), activity.getDescription(),
                activity.getContent(), activity.getHighlights(), activity.getNotices(), activity.getCoverFileId(), coverFileUrl, activity.getVenueName(), activity.getVenueAddress(), activity.getStatus(),
                activity.getRejectionReason(), activity.getReviewedBy(), activity.getReviewedAt(),activity.getSubmittedBy(),activity.getSubmittedAt(),null,null);
    }

    public static ActivityViewVO from(Activity activity) {
        return from(activity, null);
    }
}
