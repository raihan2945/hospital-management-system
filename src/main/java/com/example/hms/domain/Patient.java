package com.example.hms.domain;

import com.example.hms.domain.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Locale;

/** Patient registration and contact details. */
@Entity
@Table(name = "patients")
public class Patient extends Person {

    @Column(unique = true, length = 30)
    private String patientCode;

    @Size(max = 500)
    @Column(length = 500)
    private String address;

    @Pattern(regexp = "(?:A|B|AB|O)[+-]")
    @Column(length = 3)
    private String bloodGroup;

    @Size(max = 100)
    @Column(length = 100)
    private String emergencyContact;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @PastOrPresent
    private LocalDate dateOfBirth;

    protected Patient() {
        // Required by JPA.
    }

    public Patient(String firstName, String lastName, String phone, String email,
                   Gender gender, LocalDate dateOfBirth) {
        super(firstName, lastName, phone, email);
        updatePersonalDetails(gender, dateOfBirth);
    }

    public void updatePersonalDetails(Gender gender, LocalDate dateOfBirth) {
        if (gender == null) {
            throw new IllegalArgumentException("Gender is required; use NOT_SPECIFIED if unknown");
        }
        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void assignPatientCode() {
        if (getId() == null) {
            throw new IllegalStateException("Persist the patient before assigning a code");
        }
        if (patientCode == null) {
            patientCode = String.format(Locale.ROOT, "PAT-%06d", getId());
        }
    }

    public void updateRegistrationDetails(String address, String bloodGroup, String emergencyContact) {
        String validAddress = optionalText(address, "Address", 500);
        String validGroup = optionalText(bloodGroup, "Blood group", 3);
        String validContact = optionalText(emergencyContact, "Emergency contact", 100);
        if (validGroup != null && !validGroup.matches("(?:A|B|AB|O)[+-]")) {
            throw new IllegalArgumentException("Choose a valid blood group");
        }
        this.address = validAddress;
        this.bloodGroup = validGroup;
        this.emergencyContact = validContact;
    }

    public String getPatientCode() { return patientCode; }
    public String getAddress() { return address; }
    public String getBloodGroup() { return bloodGroup; }
    public String getEmergencyContact() { return emergencyContact; }
}
