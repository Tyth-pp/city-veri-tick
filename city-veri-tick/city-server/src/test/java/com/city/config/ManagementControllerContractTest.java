package com.city.config;

import com.city.controller.admin.organizer.OrganizerActivityController;
import com.city.controller.admin.organizer.OrganizerActivitySessionController;
import com.city.controller.admin.organizer.OrganizerTicketTypeController;
import com.city.controller.admin.platform.PlatformActivityController;
import com.city.controller.admin.platform.PlatformOrganizerController;
import com.city.controller.admin.platform.PlatformDashboardController;
import com.city.controller.admin.platform.PlatformOrderController;
import com.city.controller.admin.platform.PlatformVerificationController;
import com.city.controller.admin.platform.PlatformVerifierController;
import com.city.controller.admin.platform.PlatformUserController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Parameter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManagementControllerContractTest {
    @Test
    void managementEndpointsAreSplitByResponsibility() {
        assertPath(OrganizerActivityController.class, "/controller/admin/organizer/activities");
        assertPath(OrganizerActivitySessionController.class, "/controller/admin/organizer");
        assertPath(OrganizerTicketTypeController.class, "/controller/admin/organizer");
        assertPath(PlatformActivityController.class, "/controller/admin/platform/activities");
        assertPath(PlatformOrganizerController.class, "/controller/admin/platform/organizers");
        assertPath(PlatformDashboardController.class, "/controller/admin/platform/dashboard");
        assertPath(PlatformOrderController.class, "/controller/admin/platform/orders");
        assertPath(PlatformVerificationController.class, "/controller/admin/platform/verification");
        assertPath(PlatformVerifierController.class, "/controller/admin/platform/verifiers");
        assertPath(PlatformUserController.class, "/controller/admin/platform/users");
    }

    @Test
    void pathVariablesHaveExplicitNames() {
        List.of(OrganizerActivityController.class, OrganizerActivitySessionController.class,
                        OrganizerTicketTypeController.class, PlatformActivityController.class,
                        PlatformOrganizerController.class, PlatformDashboardController.class,
                        PlatformOrderController.class, PlatformVerificationController.class,
                        PlatformVerifierController.class,
                        PlatformUserController.class)
                .forEach(controller -> {
                    for (var method : controller.getDeclaredMethods()) {
                        for (Parameter parameter : method.getParameters()) {
                            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                            if (pathVariable != null) {
                                assertThat(pathVariable.value()).as(controller.getSimpleName() + "." + method.getName())
                                        .isNotBlank();
                            }
                        }
                    }
                });
    }

    private void assertPath(Class<?> controller, String path) {
        RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly(path);
    }
}
