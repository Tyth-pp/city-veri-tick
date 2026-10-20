package com.city.config;

import com.city.CityApplication;
import com.city.controller.admin.organizer.OrganizerActivityController;
import com.city.controller.admin.organizer.OrganizerActivitySessionController;
import com.city.controller.admin.organizer.OrganizerTicketTypeController;
import com.city.controller.admin.platform.PlatformActivityController;
import com.city.controller.admin.platform.PlatformUserController;
import com.city.controller.user.UserOrderController;
import com.city.controller.verify.VerifyTicketController;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiSecurityDocumentationTest {
    @Test
    void declaresAuthorizationHeaderSecurityScheme() {
        SecurityScheme scheme = CityApplication.class.getAnnotation(SecurityScheme.class);

        assertThat(scheme).isNotNull();
        assertThat(scheme.name()).isEqualTo("Authorization");
        assertThat(scheme.type()).isEqualTo(SecuritySchemeType.APIKEY);
        assertThat(scheme.in()).isEqualTo(SecuritySchemeIn.HEADER);
        assertThat(scheme.paramName()).isEqualTo("Authorization");
    }

    @Test
    void marksProtectedControllersAsRequiringBearerAuthentication() {
        List<Class<?>> protectedControllers = List.of(
                OrganizerActivityController.class,
                OrganizerActivitySessionController.class,
                OrganizerTicketTypeController.class,
                PlatformActivityController.class,
                PlatformUserController.class,
                UserOrderController.class,
                VerifyTicketController.class
        );

        assertThat(protectedControllers).allSatisfy(controller -> {
            SecurityRequirement requirement = controller.getAnnotation(SecurityRequirement.class);
            assertThat(requirement).isNotNull();
            assertThat(requirement.name()).isEqualTo("Authorization");
        });
    }
}
