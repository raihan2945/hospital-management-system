package com.example.hms.exception;

/** A payment rule failure shown beside the amount field. */
public class InvalidPaymentException extends BillingValidationException {
    public InvalidPaymentException(String message) { super("amount", message); }
}
