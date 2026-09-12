package com.example.hms.exception;

public class BillingValidationException extends RuntimeException {
    private final String field;
    public BillingValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
    public String getField() { return field; }
}
