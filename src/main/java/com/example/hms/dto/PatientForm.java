package com.example.hms.dto;

import com.example.hms.domain.Patient;
import com.example.hms.domain.enums.Gender;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class PatientForm extends PersonForm {
    @NotNull(message = "Select a gender")
    private Gender gender = Gender.NOT_SPECIFIED;

    @PastOrPresent(message = "Date of birth cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Pattern(regexp = "(?:A|B|AB|O)[+-]", message = "Choose a valid blood group")
    private String bloodGroup;

    @Size(max = 100, message = "Emergency contact must not exceed 100 characters")
    private String emergencyContact;

    public static PatientForm from(Patient patient) {
        PatientForm form = new PatientForm();
        form.copyContactFrom(patient);
        form.setGender(patient.getGender());
        form.setDateOfBirth(patient.getDateOfBirth());
        form.setAddress(patient.getAddress());
        form.setBloodGroup(patient.getBloodGroup());
        form.setEmergencyContact(patient.getEmergencyContact());
        return form;
    }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address == null || address.isBlank() ? null : address.strip(); }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup == null || bloodGroup.isBlank() ? null : bloodGroup.strip(); }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact == null || emergencyContact.isBlank() ? null : emergencyContact.strip(); }
}

