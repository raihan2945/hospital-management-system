package com.example.hms;

import com.example.hms.domain.*;
import com.example.hms.domain.enums.*;
import com.example.hms.dto.*;
import com.example.hms.repository.*;
import com.example.hms.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardTests {
    @Autowired DashboardService dashboard;
    @Autowired BillingService billing;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired AppointmentRepository appointments;
    @Autowired BillRepository bills;
    @Autowired PaymentRepository payments;
    @Autowired EntityManager entities;
    @Autowired Clock clock;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void emptyDatabaseShowsZeroAmountsAndUsefulEmptyStates() throws Exception {
        DashboardSummary summary = dashboard.getDashboard();
        assertThat(summary.totalPatients()).isZero();
        assertThat(summary.totalDoctors()).isZero();
        assertThat(summary.totalAppointments()).isZero();
        assertThat(summary.todayAppointments()).isZero();
        assertThat(summary.pendingAppointments()).isZero();
        assertThat(summary.completedAppointments()).isZero();
        assertThat(summary.totalBills()).isZero();
        assertThat(summary.totalRevenue()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.outstandingDue()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.recentPatients()).isEmpty();
        assertThat(summary.recentAppointments()).isEmpty();
        mvc.perform(get("/dashboard")).andExpect(status().isOk()).andExpect(view().name("dashboard/index"))
                .andExpect(content().string(containsString("No patients registered yet")))
                .andExpect(content().string(containsString("No appointments yet")))
                .andExpect(content().string(containsString("Asia/Dhaka")))
                .andExpect(content().string(not(containsString("th:text"))));
    }

    @Test
    void countsUseExactDatesAndDistinguishPendingCompletedAndCancelled() throws Exception {
        Patient patient = patient("Dashboard"); patient("Second");
        Doctor doctor = doctor();
        LocalDate today = LocalDate.now(clock);
        appointment(patient, doctor, today, 8, AppointmentStatus.SCHEDULED);
        appointment(patient, doctor, today, 9, AppointmentStatus.CONFIRMED);
        appointment(patient, doctor, today, 10, AppointmentStatus.COMPLETED);
        appointment(patient, doctor, today, 11, AppointmentStatus.CANCELLED);
        appointment(patient, doctor, today.plusDays(1), 8, AppointmentStatus.SCHEDULED);
        appointment(patient, doctor, today.minusDays(1), 8, AppointmentStatus.COMPLETED);
        entities.flush(); entities.clear();
        DashboardSummary summary = dashboard.getDashboard();
        assertThat(summary.totalPatients()).isEqualTo(2);
        assertThat(summary.totalDoctors()).isEqualTo(1);
        assertThat(summary.totalAppointments()).isEqualTo(6);
        assertThat(summary.todayAppointments()).isEqualTo(4);
        assertThat(summary.pendingAppointments()).isEqualTo(3);
        assertThat(summary.completedAppointments()).isEqualTo(2);
        assertThat(dashboard.countPatients()).isEqualTo(2);
        assertThat(dashboard.countDoctors()).isEqualTo(1);
        assertThat(dashboard.countTodayAppointments()).isEqualTo(4);
        assertThat(dashboard.countPendingAppointments()).isEqualTo(3);
        mvc.perform(get("/dashboard")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/appointments?date=" + today)))
                .andExpect(content().string(containsString("/appointments?status=SCHEDULED")))
                .andExpect(content().string(containsString("/appointments?status=CONFIRMED")))
                .andExpect(content().string(containsString("/appointments?status=COMPLETED")));
    }

    @Test
    void revenueCountsPaymentsOnceAndDueUsesRemainingBalances() throws Exception {
        Patient patient = patient("Billing");
        bill(patient, "100.25");
        Bill partial = bill(patient, "200.50");
        pay(partial, "20.10", PaymentMethod.CARD);
        pay(partial, "30.15", PaymentMethod.CASH);
        Bill paid = bill(patient, "300.75");
        pay(paid, "300.75", PaymentMethod.MOBILE_BANKING);
        bill(patient, "0.00");
        entities.flush(); entities.clear();
        DashboardSummary summary = dashboard.getDashboard();
        assertThat(summary.totalBills()).isEqualTo(4);
        assertThat(summary.totalRevenue()).isEqualTo(new BigDecimal("351.00"));
        assertThat(summary.outstandingDue()).isEqualTo(new BigDecimal("250.50"));
        assertThat(dashboard.calculateTotalRevenue()).isEqualByComparingTo("351.00");
        assertThat(dashboard.calculateOutstandingDue()).isEqualByComparingTo("250.50");
        mvc.perform(get("/dashboard")).andExpect(status().isOk())
                .andExpect(content().string(containsString("351.00")))
                .andExpect(content().string(containsString("250.50")))
                .andExpect(content().string(containsString("/billing?status=PARTIALLY_PAID")));
        pay(billing.getBill(partial.getId()), "150.25", PaymentMethod.BANK_TRANSFER);
        DashboardSummary refreshed = dashboard.getDashboard();
        assertThat(refreshed.totalRevenue()).isEqualByComparingTo("501.25");
        assertThat(refreshed.outstandingDue()).isEqualByComparingTo("100.25");
    }

    @Test
    void recentListsAreBoundedAndOrderByCreationThenIdRatherThanVisitDate() {
        Doctor doctor = doctor();
        var patientIds = new ArrayList<Long>();
        var appointmentIds = new ArrayList<Long>();
        for (int i = 0; i < 7; i++) {
            Patient patient = patient("Recent " + i);
            patientIds.add(patient.getId());
            appointmentIds.add(appointment(patient, doctor, LocalDate.now(clock).minusDays(i), 10, AppointmentStatus.SCHEDULED).getId());
        }
        // Tied creation timestamps exercise the deterministic ID tie-breaker.
        entities.createNativeQuery("update patients set created_at = :created").setParameter("created", Instant.parse("2026-01-01T00:00:00Z")).executeUpdate();
        entities.createNativeQuery("update appointments set created_at = :created").setParameter("created", Instant.parse("2026-01-01T00:00:00Z")).executeUpdate();
        entities.clear();
        DashboardSummary summary = dashboard.getDashboard();
        assertThat(summary.recentPatients()).extracting(DashboardSummary.RecentPatient::id)
                .containsExactlyElementsOf(patientIds.reversed().subList(0, 5));
        assertThat(summary.recentAppointments()).extracting(DashboardSummary.RecentAppointment::id)
                .containsExactlyElementsOf(appointmentIds.reversed().subList(0, 5));
        assertThatThrownBy(() -> summary.recentPatients().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void statusChangesAppearOnNextRefresh() {
        Patient patient = patient("Status");
        Doctor doctor = doctor();
        Appointment appointment = appointment(patient, doctor, LocalDate.now(clock), 10, AppointmentStatus.SCHEDULED);
        assertThat(dashboard.getDashboard().pendingAppointments()).isEqualTo(1);
        appointment.confirm(); appointments.flush();
        assertThat(dashboard.getDashboard().confirmedAppointments()).isEqualTo(1);
        appointment.complete(); appointments.flush();
        assertThat(dashboard.getDashboard().pendingAppointments()).isZero();
        assertThat(dashboard.getDashboard().completedAppointments()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void populatedDashboardRendersAfterTransactionClosesWithoutWriting() throws Exception {
        var transaction = new TransactionTemplate(transactionManager);
        Long[] ids = transaction.execute(status -> {
            Patient patient = patient("<script>unsafe</script>");
            Doctor doctor = doctor();
            Appointment appointment = appointment(patient, doctor, LocalDate.now(clock), 10, AppointmentStatus.CONFIRMED);
            return new Long[]{patient.getId(), doctor.getId(), appointment.getId()};
        });
        try {
            long patientCount = patients.count(), appointmentCount = appointments.count();
            for (String route : new String[]{"/", "/dashboard"}) {
                mvc.perform(get(route)).andExpect(status().isOk())
                        .andExpect(content().string(containsString("&lt;script&gt;unsafe&lt;/script&gt;")))
                        .andExpect(content().string(containsString("/patients/" + ids[0])))
                        .andExpect(content().string(containsString("/appointments/" + ids[2])))
                        .andExpect(content().string(containsString("Confirmed")));
            }
            assertThat(patients.count()).isEqualTo(patientCount);
            assertThat(appointments.count()).isEqualTo(appointmentCount);
        } finally {
            appointments.deleteById(ids[2]); patients.deleteById(ids[0]); doctors.deleteById(ids[1]);
        }
    }

    private Patient patient(String name) {
        Patient patient = patients.saveAndFlush(new Patient(name, "Patient", "01700123456", null, Gender.OTHER, null));
        patient.assignPatientCode(); patients.flush(); return patient;
    }
    private Doctor doctor() {
        Doctor doctor = doctors.saveAndFlush(new Doctor("Dashboard", "Doctor", "01800123456", null, "ENT", BigDecimal.TEN));
        doctor.assignDoctorCode(); doctors.flush(); return doctor;
    }
    private Appointment appointment(Patient patient, Doctor doctor, LocalDate date, int hour, AppointmentStatus status) {
        Appointment appointment = new Appointment(patient, doctor, date, LocalTime.of(hour, 0), null, null);
        switch (status) {
            case CONFIRMED -> appointment.confirm();
            case COMPLETED -> appointment.complete();
            case CANCELLED -> appointment.cancel();
            default -> { }
        }
        appointments.saveAndFlush(appointment); appointment.assignAppointmentCode(); appointments.flush(); return appointment;
    }
    private Bill bill(Patient patient, String amount) {
        CreateBillForm form = new CreateBillForm(); form.setPatientId(patient.getId());
        form.setConsultationFee(new BigDecimal(amount)); return billing.createBill(form);
    }
    private void pay(Bill bill, String amount, PaymentMethod method) {
        PaymentForm form = new PaymentForm(); form.setAmount(new BigDecimal(amount));
        form.setPaymentMethod(method); form.setVersion(bill.getVersion()); billing.recordPayment(bill.getId(), form);
    }
}
