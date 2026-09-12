package com.example.hms.exception;

public class DuplicateBillException extends BillingValidationException {
    public DuplicateBillException() {
        super("appointmentId", "This appointment already has an invoice. Open its existing invoice from the billing list.");
    }
}
