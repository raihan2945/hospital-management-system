package com.example.hms;

import com.example.hms.domain.*;
import com.example.hms.domain.enums.*;
import com.example.hms.dto.*;
import com.example.hms.exception.*;
import com.example.hms.repository.*;
import com.example.hms.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingConcurrencyTests {
    @Autowired BillingService service;
    @Autowired AppointmentService appointmentService;
    @Autowired BillRepository bills;
    @Autowired PaymentRepository payments;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired AppointmentRepository appointments;
    @Autowired MockMvc mvc;

    @Test
    void concurrentInvoiceCreationAndPaymentHaveOneWinnerAndPagesRenderOutsideTransactions() throws Exception {
        Patient patient = patients.saveAndFlush(new Patient("Billing race", "Patient", "01700123456", null, Gender.OTHER, null));
        Doctor doctor = doctors.saveAndFlush(new Doctor("Billing race", "Doctor", "01800123456", null, "ENT", BigDecimal.TEN));
        AppointmentForm appointmentForm = new AppointmentForm(); appointmentForm.setPatientId(patient.getId());
        appointmentForm.setDoctorId(doctor.getId()); appointmentForm.setAppointmentDate(LocalDate.of(2031, 1, 1));
        appointmentForm.setAppointmentTime(LocalTime.NOON);
        Appointment appointment = appointmentService.createAppointment(appointmentForm);
        try {
            compete(() -> {
                CreateBillForm form = new CreateBillForm(); form.setPatientId(patient.getId());
                form.setAppointmentId(appointment.getId()); form.setConsultationFee(new BigDecimal("100"));
                service.createBill(form);
            }, BillingValidationException.class);
            Bill bill = service.search(patient.getId(), appointment.getId(), null, 0).getContent().getFirst();
            assertThat(service.search(patient.getId(), null, null, 0).getTotalElements()).isEqualTo(1);
            Long version = bill.getVersion();
            compete(() -> {
                PaymentForm form = new PaymentForm(); form.setAmount(new BigDecimal("60"));
                form.setPaymentMethod(PaymentMethod.CASH); form.setVersion(version);
                service.recordPayment(bill.getId(), form);
            }, BillingStateException.class);
            Bill reloaded = service.getBill(bill.getId());
            assertThat(reloaded.getPaidAmount()).isEqualByComparingTo("60");
            assertThat(reloaded.getDueAmount()).isEqualByComparingTo("40");
            assertThat(reloaded.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
            assertThat(service.paymentHistory(bill.getId())).hasSize(1);
            mvc.perform(get("/billing")).andExpect(status().isOk());
            mvc.perform(get("/billing/" + bill.getId())).andExpect(status().isOk());
            mvc.perform(get("/billing/" + bill.getId() + "/payment")).andExpect(status().isOk());
            mvc.perform(get("/appointments/" + appointment.getId())).andExpect(status().isOk());
            mvc.perform(get("/billing/new")).andExpect(status().isOk());
        } finally {
            for (Bill bill : service.search(patient.getId(), null, null, 0)) {
                payments.deleteAll(service.paymentHistory(bill.getId()));
                bills.deleteById(bill.getId());
            }
            appointments.deleteById(appointment.getId());
            patients.deleteById(patient.getId());
            doctors.deleteById(doctor.getId());
        }
    }

    private void compete(Runnable operation, Class<? extends RuntimeException> expected) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger(), rejected = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> task = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) { throw new AssertionError("Start timed out"); }
                try { operation.run(); succeeded.incrementAndGet(); }
                catch (RuntimeException exception) {
                    if (!expected.isInstance(exception)) { throw exception; }
                    rejected.incrementAndGet();
                }
                return null;
            };
            var first = executor.submit(task); var second = executor.submit(task);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
            assertThat(succeeded.get()).isEqualTo(1); assertThat(rejected.get()).isEqualTo(1);
        }
    }
}
