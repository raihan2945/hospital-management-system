package com.example.hms;

import com.example.hms.domain.Doctor;
import com.example.hms.domain.Patient;
import com.example.hms.domain.enums.Gender;
import com.example.hms.dto.AppointmentForm;
import com.example.hms.exception.AppointmentConflictException;
import com.example.hms.repository.AppointmentRepository;
import com.example.hms.repository.DoctorRepository;
import com.example.hms.repository.PatientRepository;
import com.example.hms.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentConcurrencyTests {
    @Autowired private AppointmentService service;
    @Autowired private AppointmentRepository appointments;
    @Autowired private DoctorRepository doctors;
    @Autowired private PatientRepository patients;

    @Test
    void competingTransactionsCannotBookTheSameDoctorSlot() throws Exception {
        Patient patient = patients.saveAndFlush(new Patient("Concurrency", "Patient", "01700123456", null, Gender.NOT_SPECIFIED, null));
        Doctor doctor = doctors.saveAndFlush(new Doctor("Concurrency", "Doctor", "01800123456", null, "ENT", BigDecimal.ZERO));
        AtomicInteger booked = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Runnable book = () -> {
                ready.countDown();
                try {
                    if (!start.await(10, TimeUnit.SECONDS)) { throw new AssertionError("Booking start timed out"); }
                    AppointmentForm form = new AppointmentForm();
                    form.setPatientId(patient.getId());
                    form.setDoctorId(doctor.getId());
                    form.setAppointmentDate(LocalDate.of(2030, 1, 1));
                    form.setAppointmentTime(LocalTime.NOON);
                    service.createAppointment(form);
                    booked.incrementAndGet();
                } catch (AppointmentConflictException expected) {
                    rejected.incrementAndGet();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
            };
            var first = executor.submit(book);
            var second = executor.submit(book);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
            assertThat(booked.get()).isEqualTo(1);
            assertThat(rejected.get()).isEqualTo(1);
        } finally {
            appointments.deleteAll(appointments.findAll((root, query, cb) -> cb.equal(root.get("doctor").get("id"), doctor.getId())));
            patients.deleteById(patient.getId());
            doctors.deleteById(doctor.getId());
        }
    }
}
