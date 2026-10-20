package com.city;

import com.city.security.JwtTokenService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {
    @Test
    void createsAndParsesPrincipalClaims() {
        JwtTokenService service = new JwtTokenService(
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                Duration.ofHours(1),
                Clock.fixed(Instant.parse("2026-07-17T00:00:00Z"), ZoneOffset.UTC));

        String token = service.createToken(7L, "VERIFIER");

        assertThat(service.parse(token)).hasValueSatisfying(principal -> {
            assertThat(principal.userId()).isEqualTo(7L);
            assertThat(principal.role()).isEqualTo("VERIFIER");
            assertThat(principal.jti()).isNotBlank();
            assertThat(principal.initialPasswordOnly()).isFalse();
            assertThat(principal.expiresAt()).isEqualTo(Instant.parse("2026-07-17T01:00:00Z"));
        });
        assertThat(service.parse(service.createInitialPasswordToken(7L,"VERIFIER"))).hasValueSatisfying(principal ->
                assertThat(principal.initialPasswordOnly()).isTrue());
    }
}
