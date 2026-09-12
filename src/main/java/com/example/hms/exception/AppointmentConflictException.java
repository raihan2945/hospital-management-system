package com.example.hms.exception;

public class AppointmentConflictException extends AppointmentValidationException {
    public AppointmentConflictException() {
        super("appointmentTime", "This doctor already has an appointment at that date and time. Choose another slot.");
    }
}
