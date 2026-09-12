package com.example.hms.exception;

/** The selected patient is already booked with another doctor at the same date and time. */
public class PatientScheduleConflictException extends AppointmentValidationException {
    public PatientScheduleConflictException() {
        super("patientId", "This patient already has an appointment at that date and time. Choose another slot or select a different patient.");
    }
}
