package com.example.hms.service;

import com.example.hms.domain.Doctor;
import com.example.hms.dto.DoctorForm;
import com.example.hms.exception.ResourceNotFoundException;
import com.example.hms.repository.DirectorySearch;
import com.example.hms.repository.DoctorRepository;
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
import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class DoctorService {
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;

    public DoctorService(DoctorRepository doctors, AppointmentRepository appointments) {
        this.doctors = doctors;
        this.appointments = appointments;
    }

    public Page<Doctor> search(String keyword, String specialization, Boolean available, int page) {
        Specification<Doctor> criteria = DirectorySearch.matching(keyword, "doctorCode");
        if (specialization != null && !specialization.isBlank()) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("specialization"), specialization));
        }
        if (available != null) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("available"), available));
        }
        return doctors.findAll(criteria, PageRequest.of(Math.max(0, page), 10, Sort.by("id").descending()));
    }

    public List<String> specializations() {
        return doctors.findSpecializations();
    }

    public Doctor getDoctor(Long id) {
        return doctors.findById(id).orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
    }

    @Transactional
    public Doctor createDoctor(@Valid DoctorForm form) {
        Doctor doctor = new Doctor(form.getFirstName(), form.getLastName(), form.getPhone(),
                form.getEmail(), form.getSpecialization(), form.getConsultationFee());
        applyPractice(doctor, form);
        doctors.saveAndFlush(doctor);
        doctor.assignDoctorCode();
        return doctor;
    }

    @Transactional
    public Doctor updateDoctor(Long id, @Valid DoctorForm form) {
        Doctor doctor = doctors.findForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
        doctor.rename(form.getFirstName(), form.getLastName());
        doctor.updateContact(form.getPhone(), form.getEmail());
        doctor.updatePracticeDetails(form.getSpecialization(), form.getConsultationFee());
        applyPractice(doctor, form);
        return doctor;
    }

    private void applyPractice(Doctor doctor, DoctorForm form) {
        doctor.updateQualification(form.getQualification());
        if (form.isAvailable()) {
            doctor.markAvailable();
        } else {
            doctor.markUnavailable();
        }
    }

    @Transactional
    public void deleteDoctor(Long id) {
        Doctor doctor = doctors.findForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
        if (appointments.existsByDoctorId(id)) {
            throw new ReferencedRecordException("This doctor has appointment history and cannot be deleted. Mark the doctor unavailable instead.");
        }
        doctors.delete(doctor);
        doctors.flush();
    }
}
