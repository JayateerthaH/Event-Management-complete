package com.event.socialmodule.nudge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SavedEventNudgesTest {

    @Test
    void almostFullFromEightyPercentUntilFull() {
        assertThat(SavedEventNudges.almostFull(10L, 7)).isFalse();
        assertThat(SavedEventNudges.almostFull(10L, 8)).isTrue();
        assertThat(SavedEventNudges.almostFull(10L, 10)).isFalse();   // full: the waitlist takes over
        assertThat(SavedEventNudges.almostFull(null, 50)).isFalse();
        assertThat(SavedEventNudges.almostFull(3L, 3)).isFalse();
        assertThat(SavedEventNudges.almostFull(3L, 2)).isFalse();     // ceil(2.4) = 3
    }
}
