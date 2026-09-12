package com.example.hms;

import com.example.hms.config.DemoDataInitializer;
import com.example.hms.domain.Appointment;
import com.example.hms.domain.Bill;
import com.example.hms.domain.Doctor;
import com.example.hms.domain.Patient;
import com.example.hms.domain.Payment;
import com.example.hms.domain.enums.AppointmentStatus;
import com.example.hms.domain.enums.PaymentMethod;
import com.example.hms.domain.enums.PaymentStatus;
import com.example.hms.dto.DashboardSummary;
import com.example.hms.repository.*;
import com.example.hms.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The runner seeds this context at startup, so these tests read committed demo
 * records. A separate in-memory database keeps them away from the other suites.
 */
@SpringBootTest(properties = {
        "hospital.demo-data.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:hms_demo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoDataTests {
    @Autowired DemoDataInitializer demoData;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired AppointmentRepository appointments;
    @Autowired BillRepository bills;
    @Autowired PaymentRepository payments;
    @Autowired DashboardService dashboard;
    @Autowired Clock clock;
    @Autowired MockMvc mvc;

    @Test
    void startupAddsTheBlueprintCountsWithGeneratedCodes() {
        assertThat(doctors.count()).isEqualTo(5);
        assertThat(patients.count()).isEqualTo(10);
        assertThat(appointments.count()).isEqualTo(10);
        assertThat(bills.count()).isEqualTo(5);
        assertThat(payments.count()).isEqualTo(4);
        assertThat(doctors.findByDoctorCodeIsNull()).isEmpty();
        assertThat(patients.findByPatientCodeIsNull()).isEmpty();
        assertThat(patients.findAll()).extracting(Patient::getPatientCode).contains("PAT-000001");
        assertThat(doctors.findAll()).extracting(Doctor::getDoctorCode).contains("DOC-000001");
        assertThat(appointments.findAll()).extracting(Appointment::getAppointmentCode).doesNotContainNull();
        assertThat(bills.findAll()).extracting(Bill::getInvoiceNumber).doesNotContainNull();
    }

    @Test
    @Transactional(readOnly = true)
    void demoRecordsCoverEveryStatusSpecializationAndPaymentMethod() {
        assertThat(doctors.findSpecializations()).containsExactly(
                "Cardiology", "Dermatology", "General Medicine", "Orthopedics", "Pediatrics");
        assertThat(doctors.findAll()).extracting(Doctor::isAvailable).containsOnlyOnce(false);
        for (AppointmentStatus status : AppointmentStatus.values()) {
            assertThat(appointments.countByStatus(status)).as("appointments with status %s", status).isPositive();
        }
        assertThat(bills.findAll()).extracting(Bill::getPaymentStatus)
                .contains(PaymentStatus.PAID, PaymentStatus.PARTIALLY_PAID, PaymentStatus.UNPAID);
        assertThat(payments.findAll()).extracting(Payment::getPaymentMethod)
                .containsExactlyInAnyOrder(PaymentMethod.CARD, PaymentMethod.CASH,
                        PaymentMethod.MOBILE_BANKING, PaymentMethod.BANK_TRANSFER);
        // Three invoices belong to completed visits; two are standalone patient bills.
        assertThat(bills.findAll().stream().filter(bill -> bill.getAppointment() != null).count()).isEqualTo(3);
    }

    @Test
    void dashboardShowsPopulatedTotalsForThePresentation() throws Exception {
        LocalDate today = LocalDate.now(clock);
        DashboardSummary summary = dashboard.getDashboard();
        assertThat(summary.totalPatients()).isEqualTo(10);
        assertThat(summary.totalDoctors()).isEqualTo(5);
        assertThat(summary.totalAppointments()).isEqualTo(10);
        assertThat(summary.todayAppointments()).isEqualTo(5);
        assertThat(summary.pendingAppointments()).isEqualTo(6);
        assertThat(summary.completedAppointments()).isEqualTo(3);
        assertThat(summary.totalBills()).isEqualTo(5);
        assertThat(summary.totalRevenue()).isEqualByComparingTo("2500.50");
        assertThat(summary.outstandingDue()).isEqualByComparingTo("2075.25");
        assertThat(summary.recentPatients()).hasSize(5);
        assertThat(summary.recentAppointments()).hasSize(5);
        mvc.perform(get("/dashboard")).andExpect(status().isOk())
                .andExpect(content().string(containsString("2500.50")))
                .andExpect(content().string(containsString("2075.25")))
                .andExpect(content().string(containsString("/appointments?date=" + today)));
        mvc.perform(get("/patients")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Amina Rahman")));
        mvc.perform(get("/doctors")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Cardiology")));
    }

    @Test
    @Transactional(readOnly = true)
    void bookedSlotsAndInvoiceAmountsFollowTheSameRulesAsStaffInput() {
        List<Appointment> booked = appointments.findAll().stream()
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED).toList();
        assertThat(booked).extracting(appointment -> List.of(appointment.getDoctor().getId(),
                appointment.getAppointmentDate(), appointment.getAppointmentTime())).doesNotHaveDuplicates();
        assertThat(booked).extracting(appointment -> List.of(appointment.getPatient().getId(),
                appointment.getAppointmentDate(), appointment.getAppointmentTime())).doesNotHaveDuplicates();
        assertThat(booked).allSatisfy(appointment -> assertThat(appointment.getDoctor().isAvailable()).isTrue());
        assertThat(bills.findAll()).allSatisfy(bill -> {
            assertThat(bill.getDiscount()).isLessThanOrEqualTo(bill.getSubtotal());
            assertThat(bill.getTotalAmount()).isEqualByComparingTo(bill.getSubtotal().subtract(bill.getDiscount()));
            assertThat(bill.getPaidAmount()).isLessThanOrEqualTo(bill.getTotalAmount());
            assertThat(bill.getAppointment() == null || bill.getAppointment().getPatient().getId()
                    .equals(bill.getPatient().getId())).isTrue();
        });
    }

    @Test
    void seedingAgainLeavesAnAlreadyPopulatedDatabaseUnchanged() {
        long patientCount = patients.count();
        long appointmentCount = appointments.count();
        long paymentCount = payments.count();
        DemoDataInitializer.DemoData repeated = demoData.seed();
        assertThat(repeated.isEmpty()).isTrue();
        assertThat(repeated).isEqualTo(DemoDataInitializer.DemoData.NONE);
        assertThat(patients.count()).isEqualTo(patientCount);
        assertThat(appointments.count()).isEqualTo(appointmentCount);
        assertThat(payments.count()).isEqualTo(paymentCount);
        assertThat(bills.count()).isEqualTo(5);
    }
}
