package com.example.hms.dto;

import com.example.hms.domain.Doctor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class DoctorForm extends PersonForm {
    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Specialization must not exceed 100 characters")
    private String specialization;

    @Size(max = 255, message = "Qualification must not exceed 255 characters")
    private String qualification;

    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "0.00", message = "Consultation fee cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Enter up to 10 digits and 2 decimal places")
    private BigDecimal consultationFee;

    private boolean available = true;

    public static DoctorForm from(Doctor doctor) {
        DoctorForm form = new DoctorForm();
        form.copyContactFrom(doctor);
        form.setSpecialization(doctor.getSpecialization());
        form.setQualification(doctor.getQualification());
        form.setConsultationFee(doctor.getConsultationFee());
        form.setAvailable(doctor.isAvailable());
        return form;
    }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization == null || specialization.isBlank() ? null : specialization.strip(); }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification == null || qualification.isBlank() ? null : qualification.strip(); }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}

