package com.example.hms.dto;

import com.example.hms.domain.enums.AppointmentStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

/** Read-only dashboard values; views never need a live persistence session. */
public record DashboardSummary(
        LocalDate today, String timeZone, Instant refreshedAt,
        long totalPatients, long totalDoctors, long totalAppointments,
        long todayAppointments, long scheduledAppointments, long confirmedAppointments,
        long completedAppointments, long totalBills,
        BigDecimal totalRevenue, BigDecimal outstandingDue,
        List<RecentPatient> recentPatients, List<RecentAppointment> recentAppointments) {

    public DashboardSummary {
        recentPatients = List.copyOf(recentPatients);
        recentAppointments = List.copyOf(recentAppointments);
    }

    public long pendingAppointments() { return scheduledAppointments + confirmedAppointments; }

    public record RecentPatient(Long id, String code, String fullName, Instant registeredAt) { }
    public record RecentAppointment(Long id, String code, String patientName, String doctorName,
                                    LocalDate date, LocalTime time, AppointmentStatus status) { }
}
