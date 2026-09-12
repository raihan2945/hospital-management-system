package com.example.hms.domain;

import com.example.hms.domain.enums.Gender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PersonTests {

    @Test
    void bothKindsOfPersonInheritNameAndContactBehavior() {
        Person patient = patient();
        Person doctor = doctor();

        for (Person person : new Person[] {patient, doctor}) {
            person.rename("  Samira  ", "  Khan  ");
            person.updateContact("  +880 1700 123456  ", "  samira@example.com  ");

            assertThat(person.getFullName()).isEqualTo("Samira Khan");
            assertThat(person.getPhone()).isEqualTo("+880 1700 123456");
            assertThat(person.getEmail()).isEqualTo("samira@example.com");
            assertThat(person.getId()).isNull();
            assertThat(person.getCreatedAt()).isNull();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectedNameAndContactUpdatesPreserveExistingState(String invalid) {
        Patient patient = patient();

        assertThatIllegalArgumentException().isThrownBy(() -> patient.rename("Changed", invalid));
        assertThatIllegalArgumentException().isThrownBy(() -> patient.updateContact(invalid, "new@example.com"));

        assertThat(patient.getFullName()).isEqualTo("Amina Rahman");
        assertThat(patient.getPhone()).isEqualTo("01700123456");
        assertThat(patient.getEmail()).isNull();
    }

    @Test
    void optionalEmailIsNormalizedAndOversizedContactIsRejectedAtomically() {
        Patient patient = patient();
        patient.updateContact("01700123456", "   ");
        assertThat(patient.getEmail()).isNull();

        assertThatIllegalArgumentException().isThrownBy(
                () -> patient.updateContact("changed", "a".repeat(255)));
        assertThat(patient.getPhone()).isEqualTo("01700123456");
        assertThatIllegalArgumentException().isThrownBy(
                () -> patient.rename("a".repeat(101), "Rahman"));
    }

    @Test
    void invalidPersonalDetailsDoNotPartiallyChangePatient() {
        Patient patient = patient();
        assertThatIllegalArgumentException().isThrownBy(
                () -> patient.updatePersonalDetails(Gender.OTHER, LocalDate.now().plusDays(1)));
        assertThatIllegalArgumentException().isThrownBy(
                () -> patient.updatePersonalDetails(null, null));
        assertThat(patient.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(patient.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 15));

        patient.updatePersonalDetails(Gender.NOT_SPECIFIED, null);
        assertThat(patient.getDateOfBirth()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "1.001", "10000000000.00"})
    void rejectsFeesThatCannotBeStoredWithoutChangingTheirValue(String amount) {
        Doctor doctor = doctor();
        assertThatIllegalArgumentException().isThrownBy(
                () -> doctor.updatePracticeDetails("Cardiology", new BigDecimal(amount)));
        assertThat(doctor.getSpecialization()).isEqualTo("General Medicine");
        assertThat(doctor.getConsultationFee()).isEqualByComparingTo("500.00");
    }

    @Test
    void doctorAllowsFreeConsultationsAndExplicitAvailabilityChanges() {
        Doctor doctor = doctor();
        assertThatIllegalArgumentException().isThrownBy(
                () -> doctor.updatePracticeDetails("General Medicine", null));
        doctor.updatePracticeDetails(" General Medicine ", BigDecimal.ZERO);
        assertThat(doctor.getConsultationFee()).isEqualTo(new BigDecimal("0.00"));
        assertThat(doctor.isAvailable()).isTrue();
        doctor.markUnavailable();
        assertThat(doctor.isAvailable()).isFalse();
        doctor.markAvailable();
        assertThat(doctor.isAvailable()).isTrue();
    }

    static Patient patient() {
        return new Patient("Amina", "Rahman", "01700123456", null,
                Gender.FEMALE, LocalDate.of(2000, 1, 15));
    }

    static Doctor doctor() {
        return new Doctor("Karim", "Ahmed", "01800123456", "karim@example.com",
                "General Medicine", new BigDecimal("500.00"));
    }
}
