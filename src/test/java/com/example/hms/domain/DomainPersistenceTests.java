package com.example.hms.domain;

import com.example.hms.domain.enums.Gender;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class DomainPersistenceTests {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void inheritedFieldsAndSubclassDetailsSurviveDatabaseRoundTrip() {
        Patient patient = entityManager.persistAndFlush(PersonTests.patient());
        Doctor doctor = entityManager.persistAndFlush(PersonTests.doctor());
        Long patientId = patient.getId();
        Long doctorId = doctor.getId();
        assertThat(patientId).isPositive();
        assertThat(doctorId).isPositive();
        assertThat(patient.getCreatedAt()).isNotNull().isEqualTo(patient.getUpdatedAt());
        assertThat(doctor.getCreatedAt()).isNotNull().isEqualTo(doctor.getUpdatedAt());
        entityManager.clear();

        Patient reloadedPatient = entityManager.find(Patient.class, patientId);
        Doctor reloadedDoctor = entityManager.find(Doctor.class, doctorId);
        assertThat(reloadedPatient.getFullName()).isEqualTo("Amina Rahman");
        assertThat(reloadedPatient.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(reloadedPatient.getDateOfBirth()).isEqualTo(patient.getDateOfBirth());
        assertThat(reloadedPatient.getCreatedAt()).isEqualTo(patient.getCreatedAt());
        assertThat(reloadedDoctor.getEmail()).isEqualTo("karim@example.com");
        assertThat(reloadedDoctor.getConsultationFee()).isEqualByComparingTo("500.00");
        assertThat(reloadedDoctor.isAvailable()).isTrue();
        Object storedGender = entityManager.getEntityManager()
                .createNativeQuery("select gender from patients where id = :id")
                .setParameter("id", patientId).getSingleResult();
        assertThat(storedGender.toString()).isEqualTo("FEMALE");
    }

    @Test
    void updatingInheritedStatePreservesCreationTimeAndRefreshesUpdateTime() {
        Patient patient = entityManager.persistAndFlush(PersonTests.patient());
        Instant createdAt = patient.getCreatedAt();
        // Give the existing update time a known older value without relying on a sleep.
        entityManager.getEntityManager()
                .createNativeQuery("update patients set updated_at = :older where id = :id")
                .setParameter("older", createdAt.minusSeconds(60))
                .setParameter("id", patient.getId()).executeUpdate();
        entityManager.refresh(patient);
        Instant previousUpdate = patient.getUpdatedAt();

        patient.updateContact("01900123456", "amina@example.com");
        entityManager.flush();
        entityManager.clear();

        Patient reloaded = entityManager.find(Patient.class, patient.getId());
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.getUpdatedAt()).isAfter(previousUpdate);
        assertThat(reloaded.getPhone()).isEqualTo("01900123456");
    }

    @Test
    void inheritedBeanValidationRejectsMalformedEmailBeforePersistence() {
        Patient patient = PersonTests.patient();
        patient.updateContact("01700123456", "not-an-email");

        assertThatThrownBy(() -> entityManager.persistAndFlush(patient))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
