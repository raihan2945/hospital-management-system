package com.example.hms.domain;

import com.example.hms.domain.enums.AppointmentStatus;
import com.example.hms.exception.AppointmentStateException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appointment_doctor_slot", columnList = "doctor_id,appointment_date,appointment_time"),
        @Index(name = "idx_appointment_patient", columnList = "patient_id")
})
public class Appointment extends BaseEntity {
    @Column(unique = true, length = 30)
    private String appointmentCode;

    @Version
    private Long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @NotNull
    @Column(nullable = false)
    private LocalDate appointmentDate;

    @NotNull
    @Column(nullable = false)
    private LocalTime appointmentTime;

    @Size(max = 1000)
    @Column(length = 1000)
    private String reason;

    @Size(max = 2000)
    @Column(length = 2000)
    private String notes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    protected Appointment() { }

    public Appointment(Patient patient, Doctor doctor, LocalDate date, LocalTime time, String reason, String notes) {
        updateSchedule(patient, doctor, date, time, reason, notes);
    }

    public void updateSchedule(Patient patient, Doctor doctor, LocalDate date, LocalTime time, String reason, String notes) {
        requireEditable();
        Objects.requireNonNull(patient, "Patient is required");
        Objects.requireNonNull(doctor, "Doctor is required");
        Objects.requireNonNull(date, "Appointment date is required");
        Objects.requireNonNull(time, "Appointment time is required");
        if (time.getSecond() != 0 || time.getNano() != 0) {
            throw new IllegalArgumentException("Appointment times must use whole minutes");
        }
        String validReason = Person.optionalText(reason, "Reason", 1000);
        String validNotes = Person.optionalText(notes, "Notes", 2000);
        // Moving a confirmed booking requires confirmation of the new schedule.
        if (this.doctor != null && (!Objects.equals(this.doctor.getId(), doctor.getId())
                || !Objects.equals(this.patient.getId(), patient.getId())
                || !Objects.equals(appointmentDate, date) || !Objects.equals(appointmentTime, time))) {
            status = AppointmentStatus.SCHEDULED;
        }
        this.patient = patient;
        this.doctor = doctor;
        appointmentDate = date;
        appointmentTime = time;
        this.reason = validReason;
        this.notes = validNotes;
    }

    public void requireEditable() {
        if (!isEditable()) {
            throw new AppointmentStateException("Completed and cancelled appointments are read-only.");
        }
    }

    public void confirm() {
        if (status != AppointmentStatus.SCHEDULED) {
            throw new AppointmentStateException("Only scheduled appointments can be confirmed.");
        }
        status = AppointmentStatus.CONFIRMED;
    }

    public void cancel() {
        requireEditable();
        status = AppointmentStatus.CANCELLED;
    }

    public void complete() {
        requireEditable();
        status = AppointmentStatus.COMPLETED;
    }

    public void assignAppointmentCode() {
        if (getId() == null) {
            throw new IllegalStateException("Persist the appointment before assigning a code");
        }
        if (appointmentCode == null) {
            appointmentCode = String.format(Locale.ROOT, "APT-%06d", getId());
        }
    }

    public boolean isEditable() { return status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CONFIRMED; }
    public String getAppointmentCode() { return appointmentCode; }
    public Long getVersion() { return version; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public String getReason() { return reason; }
    public String getNotes() { return notes; }
    public AppointmentStatus getStatus() { return status; }
}
