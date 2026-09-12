package com.example.hms.service;

import com.example.hms.domain.*;
import com.example.hms.domain.enums.*;
import com.example.hms.dto.*;
import com.example.hms.exception.*;
import com.example.hms.repository.*;
import com.example.hms.service.payment.PaymentProcessor;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.*;

@Service
@Validated
@Transactional(readOnly = true)
public class BillingService {
    private final BillRepository bills;
    private final PaymentRepository payments;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final BillingCalculator calculator;
    private final Map<PaymentMethod, PaymentProcessor> processors = new EnumMap<>(PaymentMethod.class);

    public BillingService(BillRepository bills, PaymentRepository payments, PatientRepository patients,
                          AppointmentRepository appointments, BillingCalculator calculator, List<PaymentProcessor> processors) {
        this.bills = bills;
        this.payments = payments;
        this.patients = patients;
        this.appointments = appointments;
        this.calculator = calculator;
        for (PaymentProcessor processor : processors) {
            if (this.processors.put(processor.method(), processor) != null) {
                throw new IllegalStateException("Duplicate payment processor: " + processor.method());
            }
        }
        if (this.processors.size() != PaymentMethod.values().length) {
            throw new IllegalStateException("Every payment method requires a processor");
        }
    }

    public Page<Bill> search(Long patientId, Long appointmentId, PaymentStatus status, int page) {
        Specification<Bill> criteria = (root, query, cb) -> cb.conjunction();
        if (patientId != null) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (appointmentId != null) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("appointment").get("id"), appointmentId));
        }
        if (status != null) {
            criteria = criteria.and((root, query, cb) -> cb.equal(root.get("paymentStatus"), status));
        }
        return bills.findAll(criteria, PageRequest.of(Math.max(0, page), 10, Sort.by("id").descending()));
    }

    public Bill getBill(Long id) {
        return bills.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
    }
    public List<Payment> paymentHistory(Long id) { return payments.findByBillIdOrderByPaymentDateAscIdAsc(id); }
    public List<Patient> patientOptions() { return patients.findAll(Sort.by("firstName", "lastName", "id")); }
    public List<Appointment> appointmentOptions() { return appointments.findUnbilled(); }
    public Bill billForAppointment(Long id) { return bills.findByAppointmentId(id).orElse(null); }

    public CreateBillForm newForm(Long patientId, Long appointmentId) {
        CreateBillForm form = new CreateBillForm();
        form.setPatientId(patientId);
        if (patientId != null) { selectedPatient(patientId); }
        if (appointmentId != null) {
            Appointment appointment = appointments.findById(appointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
            if (patientId != null && !patientId.equals(appointment.getPatient().getId())) {
                throw new BillingStateException("The appointment belongs to a different patient.");
            }
            requireBillable(appointment);
            form.setPatientId(appointment.getPatient().getId());
            form.setAppointmentId(appointmentId);
            form.setConsultationFee(appointment.getDoctor().getConsultationFee());
        }
        return form;
    }

    @Transactional
    public Bill createBill(@Valid CreateBillForm form) {
        Appointment appointment = null;
        // Serialize against other bills and appointment edits before checking ownership.
        if (form.getAppointmentId() != null) {
            appointment = appointments.findForUpdate(form.getAppointmentId())
                    .orElseThrow(() -> new BillingValidationException("appointmentId", "This appointment no longer exists."));
            requireBillable(appointment);
        }
        Patient patient = selectedPatient(form.getPatientId());
        Bill bill = new Bill(patient, appointment, form.getConsultationFee(), form.getServiceCharge(),
                form.getMedicineCharge(), form.getOtherCharge(), form.getDiscount());
        bill.finalizeTotal(calculator.calculateTotal(bill));
        bills.saveAndFlush(bill);
        bill.assignInvoiceNumber();
        bills.flush();
        return bill;
    }

    @Transactional
    public Payment recordPayment(Long billId, @Valid PaymentForm form) {
        Bill bill = bills.findForUpdate(billId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
        if (!Objects.equals(form.getVersion(), bill.getVersion())) {
            throw new BillingStateException("This invoice has changed or this payment was already submitted. Reload the invoice and check payment history before trying again.");
        }
        Payment payment = processors.get(form.getPaymentMethod()).process(bill, form.getAmount(), form.getTransactionReference());
        bill.applyPayment(payment.getAmount());
        payments.saveAndFlush(payment);
        bills.flush();
        return payment;
    }

    private Patient selectedPatient(Long id) {
        return patients.findById(id).orElseThrow(() -> new BillingValidationException("patientId", "This patient no longer exists. Select another patient."));
    }
    private void requireBillable(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BillingValidationException("appointmentId", "Cancelled appointments cannot be billed.");
        }
        if (bills.existsByAppointmentId(appointment.getId())) {
            throw new BillingValidationException("appointmentId", "This appointment already has an invoice. Open its existing invoice from the billing list.");
        }
    }
}
