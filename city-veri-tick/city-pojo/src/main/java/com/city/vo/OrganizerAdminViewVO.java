package com.city.vo;

import com.city.entity.Organizer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrganizerAdminViewVO implements Serializable {
    private Long id;
    private String name;
    private long administratorCount;
    private long activityCount;
    private String unifiedSocialCreditCode;
    private String contactName;
    private String contactPhone;
    private String status;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrganizerAdminViewVO from(Organizer organizer) {
        OrganizerAdminViewVO vo = new OrganizerAdminViewVO();
        vo.id = organizer.getId();
        vo.name = organizer.getName();
        vo.unifiedSocialCreditCode = organizer.getUnifiedSocialCreditCode();
        vo.contactName = organizer.getContactName();
        vo.contactPhone = organizer.getContactPhone();
        vo.status = organizer.getStatus();
        vo.reviewedBy = organizer.getReviewedBy();
        vo.reviewedAt = organizer.getReviewedAt();
        vo.createdAt = organizer.getCreatedAt();
        vo.updatedAt = organizer.getUpdatedAt();
        return vo;
    }
}
