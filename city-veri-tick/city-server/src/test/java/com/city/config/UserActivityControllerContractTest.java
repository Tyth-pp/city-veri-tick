package com.city.config;
import java.util.List;

import com.city.controller.user.UserActivityController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class UserActivityControllerContractTest {
    @Test
    void exposesPublicActivityListDetailAndReadOnlyPreviewEndpoints() {
        RequestMapping mapping = UserActivityController.class.getAnnotation(RequestMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/controller/user/activities");
        assertThat(UserActivityController.class.getAnnotation(SecurityRequirement.class)).isNull();
        assertThat(Arrays.stream(UserActivityController.class.getDeclaredMethods())
                .filter(method -> method.getAnnotation(GetMapping.class) != null)
                .map(method -> Arrays.asList(method.getAnnotation(GetMapping.class).value())))
                .containsExactlyInAnyOrder(List.of(), List.of("/{activityId}"),
                        List.of("/{activityId}/preview"));
    }
}
