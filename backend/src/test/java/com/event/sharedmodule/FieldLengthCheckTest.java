package com.event.sharedmodule;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldLengthCheckTest {

    static class Sample {
        String venueName;                                  // JPA default: 255
        @Column(length = 10) String code;
        @Column(columnDefinition = "TEXT") String description;
        static String IGNORED = "x".repeat(1000);
    }

    private final FieldLengthCheck check = new FieldLengthCheck();

    @Test
    void namesTheFieldAndTheLimit() {
        Sample s = new Sample();
        s.venueName = "v".repeat(300);
        assertThatThrownBy(() -> check.check(s))
                .isInstanceOf(FieldTooLongException.class)
                .hasMessage("Venue name is too long: 300 characters (maximum 255).");
    }

    @Test
    void usesTheDeclaredLength() {
        Sample s = new Sample();
        s.code = "c".repeat(11);
        assertThatThrownBy(() -> check.check(s)).hasMessage("Code is too long: 11 characters (maximum 10).");
    }

    @Test
    void textColumnsAndValuesWithinLimitsPass() {
        Sample s = new Sample();
        s.venueName = "v".repeat(255);
        s.code = "c".repeat(10);
        s.description = "d".repeat(10_000);
        assertThatCode(() -> check.check(s)).doesNotThrowAnyException();
    }

    @Test
    void labelsCamelCaseFields() {
        assertThat(FieldLengthCheck.label("costOfRegistration")).isEqualTo("Cost of registration");
        assertThat(FieldLengthCheck.label("name")).isEqualTo("Name");
    }
}
