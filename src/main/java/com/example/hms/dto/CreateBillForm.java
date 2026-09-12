package com.example.hms.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateBillForm {
    @NotNull(message = "Select a patient.") @Positive
    private Long patientId;
    @Positive
    private Long appointmentId;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
    private BigDecimal serviceCharge = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
    private BigDecimal medicineCharge = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
    private BigDecimal otherCharge = BigDecimal.ZERO;
    @NotNull @DecimalMin("0.00") @Digits(integer = 11, fraction = 2)
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
