package com.example.hms.exception;

/** A scheduling error that can be shown beside the relevant form field. */
public class AppointmentValidationException extends RuntimeException {
    private final String field;

    public AppointmentValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
