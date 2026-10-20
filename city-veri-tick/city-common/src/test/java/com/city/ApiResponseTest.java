package com.city;

import com.city.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {
    @Test
    void successContainsPayloadAndStandardStatus() {
        Result<String> response = Result.success("ticket");

        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getMsg()).isNull();
        assertThat(response.getData()).isEqualTo("ticket");
    }

    @Test
    void failureContainsBusinessStatusWithoutPayload() {
        Result<Void> response = Result.error("Ticket has already been checked in");

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isNull();
    }
}
