package com.city.config;

import com.city.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManagementRoleContractTest {
    @Test
    void exposesExplicitManagementRoles() {
        assertThat(UserRole.values()).extracting(Enum::name)
                .containsExactly("PLATFORM_ADMIN", "ORGANIZER_ADMIN", "USER", "VERIFIER");
    }
}
