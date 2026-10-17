package com.event.sharedmodule;

import lombok.Getter;

/** A text field exceeds its column length; reported to the client as 400 with the field name. */
@Getter
public class FieldTooLongException extends RuntimeException {

    private final String field;

    public FieldTooLongException(String field, int length, int max) {
        super(field + " is too long: " + length + " characters (maximum " + max + ").");
        this.field = field;
    }
}
