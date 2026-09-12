package com.example.hms.dto;

import com.example.hms.domain.Appointment;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentForm {
    @NotNull(message = "Select a patient")
    @Positive(message = "Select a valid patient")
    private Long patientId;

    @NotNull(message = "Select a doctor")
    @Positive(message = "Select a valid doctor")
    private Long doctorId;

    @NotNull(message = "Appointment date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate appointmentDate;

    @NotNull(message = "Appointment time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime appointmentTime;

    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    private Long version;

    @AssertTrue(message = "Appointment time must use whole minutes (HH:mm)")
    public boolean isMinutePrecision() {
        return appointmentTime == null || (appointmentTime.getSecond() == 0 && appointmentTime.getNano() == 0);
    }

    public static AppointmentForm from(Appointment appointment) {
        AppointmentForm form = new AppointmentForm();
        form.patientId = appointment.getPatient().getId();
        form.doctorId = appointment.getDoctor().getId();
        form.appointmentDate = appointment.getAppointmentDate();
        form.appointmentTime = appointment.getAppointmentTime();
        form.reason = appointment.getReason();
        form.notes = appointment.getNotes();
        form.version = appointment.getVersion();
        return form;
    }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate date) { appointmentDate = date; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalTime time) { appointmentTime = time; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason == null || reason.isBlank() ? null : reason.strip(); }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes == null || notes.isBlank() ? null : notes.strip(); }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
