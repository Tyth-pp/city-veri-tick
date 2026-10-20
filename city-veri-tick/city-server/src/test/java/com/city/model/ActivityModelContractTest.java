package com.city.model;

import com.city.dto.CreateActivityDTO;
import com.city.entity.Activity;
import com.city.enums.ActivityStatus;
import com.city.enums.ActivityCategory;
import com.city.vo.ActivityViewVO;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityModelContractTest {
    @Test
    void activityUsesOneDocumentedStatusModel() {
        assertThat(ActivityStatus.values()).extracting(Enum::name).containsExactly(
                "DRAFT", "PENDING", "PUBLISHED", "REJECTED", "OFFLINE", "CANCELLED");
        Schema schema = ActivityStatus.class.getAnnotation(Schema.class);
        assertThat(schema).isNotNull();
        assertThat(schema.description()).contains("DRAFT", "PENDING", "PUBLISHED", "REJECTED", "OFFLINE");
        assertThat(fieldNames(Activity.class)).contains("status").doesNotContain("published");
        assertThat(fieldNames(ActivityViewVO.class))
                .contains("status", "rejectionReason", "reviewedBy", "reviewedAt")
                .doesNotContain("published");
        assertThat(fieldNames(CreateActivityDTO.class)).doesNotContain("organizerId");
    }

    @Test
    void activityExposesCategoryAndStructuredDescriptionFields() {
        assertThat(ActivityCategory.values()).extracting(Enum::name).containsExactly(
                "MOVIE", "CONCERT", "PERFORMANCE", "STAND_UP", "EXHIBITION_ART",
                "MUSIC_FESTIVAL", "OPERA", "SPORTS", "THEATRE_OPERA");
        assertThat(fieldNames(Activity.class)).contains("category", "content", "highlights", "notices");
        assertThat(fieldNames(CreateActivityDTO.class)).contains("category", "content", "highlights", "notices");
        assertThat(fieldNames(ActivityViewVO.class)).contains("category", "content", "highlights", "notices");
    }

    private String[] fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(field -> field.getName()).toArray(String[]::new);
    }
}
