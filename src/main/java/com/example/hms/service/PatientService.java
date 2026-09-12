package com.example.hms.service;

import com.example.hms.domain.Patient;
import com.example.hms.domain.enums.Gender;
import com.example.hms.dto.PatientForm;
import com.example.hms.exception.ResourceNotFoundException;
import com.example.hms.repository.DirectorySearch;
import com.example.hms.repository.PatientRepository;
import com.example.hms.repository.AppointmentRepository;
import com.example.hms.exception.ReferencedRecordException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class PatientService {
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final com.example.hms.repository.BillRepository bills;
    private final java.time.Clock clock;

    public PatientService(PatientRepository patients, AppointmentRepository appointments, com.example.hms.repository.BillRepository bills, java.time.Clock clock) {
        this.patients = patients;
        this.appointments = appointments;
        this.bills = bills;
        this.clock = clock;
    }

    public Page<Patient> search(String keyword, Gender gender, String bloodGroup, int page) {
        Specification<Patient> criteria = DirectorySearch.matching(keyword, "patientCode");
        if (gender != null) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("gender"), gender));
        }
        if (bloodGroup != null && !bloodGroup.isBlank()) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("bloodGroup"), bloodGroup));
        }
        return patients.findAll(criteria, PageRequest.of(com.example.hms.util.PageNumbers.validate(page, 10), 10, Sort.by("id").descending()));
    }

    public Patient getPatient(Long id) {
        return patients.findById(id).orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
    }

    @Transactional
    public Patient createPatient(@jakarta.validation.constraints.NotNull(message = "Form details are required.") @Valid PatientForm form) {
        Patient patient = new Patient(form.getFirstName(), form.getLastName(), form.getPhone(),
                form.getEmail(), form.getGender(), form.getDateOfBirth(), java.time.LocalDate.now(clock));
        patient.updateRegistrationDetails(form.getAddress(), form.getBloodGroup(), form.getEmergencyContact());
        patients.saveAndFlush(patient);
        patient.assignPatientCode();
        return patient;
    }

    @Transactional
    public Patient updatePatient(Long id, @jakarta.validation.constraints.NotNull(message = "Form details are required.") @Valid PatientForm form) {
        Patient patient = getPatient(id);
        patient.rename(form.getFirstName(), form.getLastName());
        patient.updateContact(form.getPhone(), form.getEmail());
        patient.updatePersonalDetails(form.getGender(), form.getDateOfBirth(), java.time.LocalDate.now(clock));
        patient.updateRegistrationDetails(form.getAddress(), form.getBloodGroup(), form.getEmergencyContact());
        return patient;
    }

    @Transactional
    public void deletePatient(Long id) {
        Patient patient = getPatient(id);
        if (bills.existsByPatientId(id)) {
            throw new ReferencedRecordException("This patient has billing history and cannot be deleted.");
        }
        if (appointments.existsByPatientId(id)) {
            throw new ReferencedRecordException("This patient has appointment history and cannot be deleted.");
        }
        patients.delete(patient);
        patients.flush();
    }
}
