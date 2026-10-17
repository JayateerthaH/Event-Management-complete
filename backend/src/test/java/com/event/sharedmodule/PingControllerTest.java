package com.event.sharedmodule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PingControllerTest {

    @Test
    void pingAnswersOkAndIsNeverCached() {
        var response = new PingController().ping();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "ok").containsKey("time");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }
}
