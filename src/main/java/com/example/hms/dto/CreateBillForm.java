package com.example.hms.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@com.example.hms.validation.ValidBillDiscount
public class CreateBillForm {
    @NotNull(message = "Select a patient.") @Positive(message = "Select a valid patient.")
    private Long patientId;
    @Positive(message = "Select a valid appointment or leave it blank.")
    private Long appointmentId;
    @NotNull(message = "Consultation fee is required.") @DecimalMin(value = "0.00", message = "Consultation fee cannot be negative.") @Digits(integer = 10, fraction = 2, message = "Consultation fee must be at most 9999999999.99 with no more than two decimal places.")
    private BigDecimal consultationFee = BigDecimal.ZERO;
    @NotNull(message = "Service charge is required.") @DecimalMin(value = "0.00", message = "Service charge cannot be negative.") @Digits(integer = 10, fraction = 2, message = "Service charge must be at most 9999999999.99 with no more than two decimal places.")
    private BigDecimal serviceCharge = BigDecimal.ZERO;
    @NotNull(message = "Medicine charge is required.") @DecimalMin(value = "0.00", message = "Medicine charge cannot be negative.") @Digits(integer = 10, fraction = 2, message = "Medicine charge must be at most 9999999999.99 with no more than two decimal places.")
    private BigDecimal medicineCharge = BigDecimal.ZERO;
    @NotNull(message = "Other charge is required.") @DecimalMin(value = "0.00", message = "Other charge cannot be negative.") @Digits(integer = 10, fraction = 2, message = "Other charge must be at most 9999999999.99 with no more than two decimal places.")
    private BigDecimal otherCharge = BigDecimal.ZERO;
    @NotNull(message = "Discount is required.") @DecimalMin(value = "0.00", message = "Discount cannot be negative.") @Digits(integer = 11, fraction = 2, message = "Discount must be at most 99999999999.99 with no more than two decimal places.")
    private BigDecimal discount = BigDecimal.ZERO;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal value) { consultationFee = value; }
    public BigDecimal getServiceCharge() { return serviceCharge; }
    public void setServiceCharge(BigDecimal value) { serviceCharge = value; }
    public BigDecimal getMedicineCharge() { return medicineCharge; }
    public void setMedicineCharge(BigDecimal value) { medicineCharge = value; }
    public BigDecimal getOtherCharge() { return otherCharge; }
    public void setOtherCharge(BigDecimal value) { otherCharge = value; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal value) { discount = value; }
}
