package com.example.hms.domain;

import com.example.hms.domain.enums.PaymentStatus;
import com.example.hms.exception.BillingValidationException;
import com.example.hms.util.Money;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "bills", indexes = @Index(name = "idx_bill_patient", columnList = "patient_id"))
public class Bill extends BaseEntity {
    @Column(unique = true, length = 30)
    private String invoiceNumber;
    @Version
    private Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", unique = true)
    private Appointment appointment;
    // Preserve the invoice recipient even when the directory profile changes later.
    @Column(nullable = false, length = 201)
    private String patientName;
    @Column(length = 30)
    private String patientCode;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal consultationFee;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal serviceCharge;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal medicineCharge;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal otherCharge;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discount;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = new BigDecimal("0.00");
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    protected Bill() { }

    public Bill(Patient patient, Appointment appointment, BigDecimal consultationFee, BigDecimal serviceCharge,
                BigDecimal medicineCharge, BigDecimal otherCharge, BigDecimal discount) {
        this.patient = Objects.requireNonNull(patient);
        if (appointment != null && !Objects.equals(appointment.getPatient().getId(), patient.getId())) {
            throw new BillingValidationException("appointmentId", "The appointment must belong to the selected patient.");
        }
        this.appointment = appointment;
        patientName = patient.getFullName();
        patientCode = patient.getPatientCode();
        this.consultationFee = Money.nonNegative(consultationFee, "consultationFee");
        this.serviceCharge = Money.nonNegative(serviceCharge, "serviceCharge");
        this.medicineCharge = Money.nonNegative(medicineCharge, "medicineCharge");
        this.otherCharge = Money.nonNegative(otherCharge, "otherCharge");
        this.discount = Money.nonNegative(discount, "discount");
        if (this.discount.compareTo(getSubtotal()) > 0) {
            throw new BillingValidationException("discount", "Discount cannot exceed the subtotal.");
        }
    }

    /** Finalize once using the injected billing calculator before persistence. */
    public void finalizeTotal(BigDecimal calculatedTotal) {
        if (totalAmount != null) { throw new IllegalStateException("An issued invoice cannot be recalculated"); }
        BigDecimal total = Money.nonNegative(calculatedTotal, "discount");
        if (total.compareTo(getSubtotal().subtract(discount)) != 0) {
            throw new IllegalArgumentException("Calculated total does not match invoice charges");
        }
        totalAmount = total;
        updatePaymentStatus();
    }

    public void applyPayment(BigDecimal amount) {
        BigDecimal valid = Money.nonNegative(amount, "amount");
        if (valid.signum() == 0 || valid.compareTo(getDueAmount()) > 0) {
            throw new com.example.hms.exception.InvalidPaymentException("Payment must be greater than zero and cannot exceed the remaining balance.");
        }
        paidAmount = paidAmount.add(valid);
        updatePaymentStatus();
    }

    private void updatePaymentStatus() {
        paymentStatus = getDueAmount().signum() == 0 ? PaymentStatus.PAID
                : paidAmount.signum() == 0 ? PaymentStatus.UNPAID : PaymentStatus.PARTIALLY_PAID;
    }

    public void assignInvoiceNumber() {
        if (getId() == null) { throw new IllegalStateException("Persist the bill before assigning its invoice number"); }
        if (invoiceNumber == null) { invoiceNumber = String.format(Locale.ROOT, "INV-%06d", getId()); }
    }

    public String getInvoiceNumber() { return invoiceNumber; }
    public Long getVersion() { return version; }
    public Patient getPatient() { return patient; }
    public Appointment getAppointment() { return appointment; }
    public String getPatientName() { return patientName; }
    public String getPatientCode() { return patientCode; }
    public BigDecimal getConsultationFee() { return consultationFee; }
    public BigDecimal getServiceCharge() { return serviceCharge; }
    public BigDecimal getMedicineCharge() { return medicineCharge; }
    public BigDecimal getOtherCharge() { return otherCharge; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getSubtotal() { return consultationFee.add(serviceCharge).add(medicineCharge).add(otherCharge); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getDueAmount() { return totalAmount.subtract(paidAmount); }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
}
