package com.example.hms.repository;

import com.example.hms.domain.Patient;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.id = :id")
    Optional<Patient> findForUpdate(Long id);

    List<Patient> findTop5ByOrderByCreatedAtDescIdDesc();
    List<Patient> findByPatientCodeIsNull();
}
