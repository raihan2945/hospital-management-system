package com.example.hms.service;

import com.example.hms.domain.Appointment;
import com.example.hms.domain.Doctor;
import com.example.hms.domain.Patient;
import com.example.hms.domain.enums.AppointmentStatus;
import com.example.hms.dto.AppointmentForm;
import com.example.hms.exception.*;
import com.example.hms.repository.AppointmentRepository;
import com.example.hms.repository.DoctorRepository;
import com.example.hms.repository.PatientRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
@Validated
@Transactional(readOnly = true)
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final com.example.hms.repository.BillRepository bills;

    public AppointmentService(AppointmentRepository appointments, PatientRepository patients, DoctorRepository doctors,
                              com.example.hms.repository.BillRepository bills) {
        this.appointments = appointments;
        this.patients = patients;
        this.doctors = doctors;
        this.bills = bills;
    }

    public Page<Appointment> search(LocalDate date, Long patientId, Long doctorId, AppointmentStatus status, int page) {
        Specification<Appointment> criteria = (root, query, cb) -> cb.conjunction();
        if (date != null) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("appointmentDate"), date));
        }
        if (patientId != null) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (doctorId != null) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("doctor").get("id"), doctorId));
        }
        if (status != null) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return appointments.findAll(criteria, PageRequest.of(com.example.hms.util.PageNumbers.validate(page, 10), 10,
                Sort.by("appointmentDate", "appointmentTime", "id").descending()));
    }

    public Appointment getAppointment(Long id) {
        return appointments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
    }

    public List<Patient> patientOptions() { return patients.findAll(Sort.by("firstName", "lastName", "id")); }
    public List<Doctor> doctorOptions() { return doctors.findAll(Sort.by("firstName", "lastName", "id")); }

    public java.util.Set<Long> billedAppointmentIds(List<Long> ids) {
        return ids.isEmpty() ? java.util.Set.of() : bills.findBilledAppointmentIds(ids);
    }

    /** Advisory read. Creation and updates repeat this check under a doctor row lock. */
    public boolean isDoctorAvailable(Long doctorId, LocalDate date, LocalTime time) {
        if (doctorId == null || date == null || time == null || time.getSecond() != 0 || time.getNano() != 0) {
            return false;
        }
        return doctors.findById(doctorId).filter(Doctor::isAvailable).isPresent()
                && !appointments.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        doctorId, date, time, AppointmentStatus.CANCELLED);
    }

    @Transactional
    public Appointment createAppointment(@jakarta.validation.constraints.NotNull(message = "Form details are required.") @Valid AppointmentForm form) {
        Patient patient = selectedPatient(form.getPatientId());
        Doctor doctor = lockDoctor(form.getDoctorId());
        requireAvailable(doctor);
        requireFreeSlot(doctor.getId(), form, null);
        Appointment appointment = new Appointment(patient, doctor, form.getAppointmentDate(),
                form.getAppointmentTime(), form.getReason(), form.getNotes());
        appointments.saveAndFlush(appointment);
        appointment.assignAppointmentCode();
        appointments.flush();
        return appointment;
    }

    @Transactional
    public Appointment updateAppointment(Long id, @jakarta.validation.constraints.NotNull(message = "Form details are required.") @Valid AppointmentForm form) {
        Appointment appointment = lockedAppointment(id, form.getVersion());
        requireUnbilled(id);
        appointment.requireEditable();
        Long previousDoctorId = appointment.getDoctor().getId();
        // All operations moving between doctors acquire their locks in ID order.
        Doctor doctor;
        if (previousDoctorId.equals(form.getDoctorId())) {
            doctor = lockDoctor(previousDoctorId);
        } else if (previousDoctorId < form.getDoctorId()) {
            lockDoctor(previousDoctorId);
            doctor = lockDoctor(form.getDoctorId());
        } else {
            doctor = lockDoctor(form.getDoctorId());
            lockDoctor(previousDoctorId);
        }
        boolean unchangedSlot = previousDoctorId.equals(doctor.getId())
                && appointment.getPatient().getId().equals(form.getPatientId())
                && appointment.getAppointmentDate().equals(form.getAppointmentDate())
                && appointment.getAppointmentTime().equals(form.getAppointmentTime());
        // Existing bookings can still have their notes corrected if a doctor becomes unavailable.
        if (!unchangedSlot) { requireAvailable(doctor); }
        Patient patient = selectedPatient(form.getPatientId());
        requireFreeSlot(doctor.getId(), form, id);
        appointment.updateSchedule(patient, doctor, form.getAppointmentDate(), form.getAppointmentTime(),
                form.getReason(), form.getNotes());
        appointments.flush();
        return appointment;
    }

    @Transactional
    public void confirmAppointment(Long id, Long version) {
        Appointment appointment = lockedAppointment(id, version);
        lockDoctor(appointment.getDoctor().getId());
        appointment.confirm();
        appointments.flush();
    }

    @Transactional
    public void cancelAppointment(Long id, Long version) {
        Appointment appointment = lockedAppointment(id, version);
        requireUnbilled(id);
        lockDoctor(appointment.getDoctor().getId());
        appointment.cancel();
        appointments.flush();
    }

    @Transactional
    public void completeAppointment(Long id, Long version) {
        Appointment appointment = lockedAppointment(id, version);
        lockDoctor(appointment.getDoctor().getId());
        appointment.complete();
        appointments.flush();
    }

    public void requireUnbilled(Long id) {
        if (bills.existsByAppointmentId(id)) {
            throw new AppointmentStateException("This appointment has an invoice and cannot be edited or cancelled. It can still be confirmed or completed.");
        }
    }

    private Appointment lockedAppointment(Long id, Long version) {
        Appointment appointment = appointments.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
        if (version == null || !Objects.equals(appointment.getVersion(), version)) {
            throw new AppointmentStateException("This appointment has changed. Reload its details before trying again.");
        }
        return appointment;
    }

    private Patient selectedPatient(Long id) {
        return patients.findById(id).orElseThrow(
                () -> new AppointmentValidationException("patientId", "This patient no longer exists. Select another patient."));
    }

    private Doctor lockDoctor(Long id) {
        return doctors.findForUpdate(id).orElseThrow(
                () -> new AppointmentValidationException("doctorId", "This doctor no longer exists. Select another doctor."));
    }

    private void requireAvailable(Doctor doctor) {
        if (!doctor.isAvailable()) {
            throw new AppointmentValidationException("doctorId", "This doctor is unavailable for new bookings. Select another doctor.");
        }
    }

    private void requireFreeSlot(Long doctorId, AppointmentForm form, Long excludedId) {
        boolean conflict = excludedId == null
                ? appointments.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        doctorId, form.getAppointmentDate(), form.getAppointmentTime(), AppointmentStatus.CANCELLED)
                : appointments.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
                        doctorId, form.getAppointmentDate(), form.getAppointmentTime(), AppointmentStatus.CANCELLED, excludedId);
        if (conflict) { throw new AppointmentConflictException(); }
    }
}
