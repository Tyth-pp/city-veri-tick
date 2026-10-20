package com.city.vo;

import com.city.entity.Activity;
import com.city.enums.ActivityCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class UserActivityDetailVO implements Serializable {
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
    private List<UserActivitySessionVO> sessions;

    public static UserActivityDetailVO from(Activity activity, String coverFileUrl, List<UserActivitySessionVO> sessions) {
        return new UserActivityDetailVO(activity.getId(), activity.getTitle(), activity.getCategory(), activity.getDescription(),
                activity.getContent(), activity.getHighlights(), activity.getNotices(), activity.getCoverFileId(), coverFileUrl, activity.getVenueName(), activity.getVenueAddress(), sessions);
    }

    public static UserActivityDetailVO from(Activity activity, List<UserActivitySessionVO> sessions) {
        return from(activity, null, sessions);
    }
}
