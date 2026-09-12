package com.example.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Locale;

/** Doctor registration, practice details, and availability. */
@Entity
@Table(name = "doctors")
public class Doctor extends Person {

    @Column(unique = true, length = 30)
    private String doctorCode;

    @Size(max = 255)
    @Column(length = 255)
    private String qualification;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String specialization;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal consultationFee;

    @Column(nullable = false)
    private boolean available = true;

    protected Doctor() {
        // Required by JPA.
    }

    public Doctor(String firstName, String lastName, String phone, String email,
                  String specialization, BigDecimal consultationFee) {
        super(firstName, lastName, phone, email);
        updatePracticeDetails(specialization, consultationFee);
    }

    public void updatePracticeDetails(String specialization, BigDecimal consultationFee) {
        String validSpecialization = requiredText(specialization, "Specialization", 100);
        if (consultationFee == null || consultationFee.signum() < 0
                || consultationFee.compareTo(new BigDecimal("9999999999.99")) > 0
                || consultationFee.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "Consultation fee must be between 0 and 9999999999.99 with at most two decimal places");
        }
        this.specialization = validSpecialization;
        this.consultationFee = consultationFee.setScale(2);
    }

    public void markAvailable() {
        available = true;
    }

    public void markUnavailable() {
        available = false;
    }

    public String getSpecialization() {
        return specialization;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public boolean isAvailable() {
        return available;
    }

    public void assignDoctorCode() {
        if (getId() == null) {
            throw new IllegalStateException("Persist the doctor before assigning a code");
        }
        if (doctorCode == null) {
            doctorCode = String.format(Locale.ROOT, "DOC-%06d", getId());
        }
    }

    public void updateQualification(String qualification) {
        this.qualification = optionalText(qualification, "Qualification", 255);
    }

    public String getDoctorCode() { return doctorCode; }
    public String getQualification() { return qualification; }
}
