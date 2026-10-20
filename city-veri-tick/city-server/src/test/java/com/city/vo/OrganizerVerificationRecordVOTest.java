package com.city.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizerVerificationRecordVOTest {
    @Test
    void carriesActivityCoverUrlForManagementLists() {
        OrganizerVerificationRecordVO view = new OrganizerVerificationRecordVO();
        view.setCoverFileUrl("https://cdn.example.com/activity.jpg");

        assertThat(view.getCoverFileUrl()).isEqualTo("https://cdn.example.com/activity.jpg");
    }
}
