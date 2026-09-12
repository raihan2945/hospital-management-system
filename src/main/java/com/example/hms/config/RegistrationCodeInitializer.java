package com.example.hms.config;

import com.example.hms.repository.DoctorRepository;
import com.example.hms.repository.PatientRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adds deterministic codes to existing phase 3 records without replacing them. */
@Component
@org.springframework.core.annotation.Order(10)
public class RegistrationCodeInitializer implements ApplicationRunner {
    private final PatientRepository patients;
    private final DoctorRepository doctors;

    public RegistrationCodeInitializer(PatientRepository patients, DoctorRepository doctors) {
        this.patients = patients;
        this.doctors = doctors;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        patients.findByPatientCodeIsNull().forEach(patient -> patient.assignPatientCode());
        doctors.findByDoctorCodeIsNull().forEach(doctor -> doctor.assignDoctorCode());
    }
}
