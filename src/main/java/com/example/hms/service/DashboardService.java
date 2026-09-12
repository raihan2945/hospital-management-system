package com.example.hms.service;

import com.example.hms.domain.enums.AppointmentStatus;
import com.example.hms.dto.DashboardSummary;
import com.example.hms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final BillRepository bills;
    private final PaymentRepository payments;
    private final Clock clock;

    public DashboardService(PatientRepository patients, DoctorRepository doctors, AppointmentRepository appointments,
                            BillRepository bills, PaymentRepository payments, Clock clock) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.bills = bills;
        this.payments = payments;
        this.clock = clock;
    }

    public long countPatients() { return patients.count(); }
    public long countDoctors() { return doctors.count(); }
    public long countTodayAppointments() { return appointments.countByAppointmentDate(LocalDate.now(clock)); }
    public long countPendingAppointments() {
        return appointments.countByStatusIn(List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED));
    }
    public BigDecimal calculateTotalRevenue() { return amount(payments.sumCollectedAmount()); }
    public BigDecimal calculateOutstandingDue() { return amount(bills.sumOutstandingDue()); }

    /** PostgreSQL supplies one consistent snapshot across counts, sums, and recent lists. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardSummary getDashboard() {
        var refreshedAt = clock.instant();
        var today = refreshedAt.atZone(clock.getZone()).toLocalDate();
        var recentPatients = patients.findTop5ByOrderByCreatedAtDescIdDesc().stream()
                .map(patient -> new DashboardSummary.RecentPatient(patient.getId(), patient.getPatientCode(),
                        patient.getFullName(), patient.getCreatedAt())).toList();
        var recentAppointments = appointments.findTop5ByOrderByCreatedAtDescIdDesc().stream()
                .map(appointment -> new DashboardSummary.RecentAppointment(appointment.getId(), appointment.getAppointmentCode(),
                        appointment.getPatient().getFullName(), appointment.getDoctor().getFullName(),
                        appointment.getAppointmentDate(), appointment.getAppointmentTime(), appointment.getStatus())).toList();
        return new DashboardSummary(today, clock.getZone().getId(), refreshedAt,
                countPatients(), countDoctors(), appointments.count(), appointments.countByAppointmentDate(today),
                appointments.countByStatus(AppointmentStatus.SCHEDULED), appointments.countByStatus(AppointmentStatus.CONFIRMED),
                appointments.countByStatus(AppointmentStatus.COMPLETED), bills.count(), calculateTotalRevenue(),
                calculateOutstandingDue(), recentPatients, recentAppointments);
    }

    private BigDecimal amount(BigDecimal value) { return value == null ? new BigDecimal("0.00") : value.setScale(2); }
}
