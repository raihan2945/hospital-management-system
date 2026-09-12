package com.example.hms;

import com.example.hms.config.HospitalTimeConfiguration;
import com.example.hms.repository.*;
import com.example.hms.service.DashboardService;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DashboardTimeTests {
    @Test
    void todayFollowsHospitalMidnightInsteadOfContainerUtc() {
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        Instant boundary = Instant.parse("2026-09-12T18:00:00Z");
        for (Instant instant : new Instant[]{boundary.minusSeconds(1), boundary}) {
            Clock clock = Clock.fixed(instant, ZoneId.of("Asia/Dhaka"));
            DashboardService service = new DashboardService(mock(PatientRepository.class), mock(DoctorRepository.class),
                    appointments, mock(BillRepository.class), mock(PaymentRepository.class), clock);
            service.countTodayAppointments();
        }
        verify(appointments).countByAppointmentDate(LocalDate.of(2026, 9, 12));
        verify(appointments).countByAppointmentDate(LocalDate.of(2026, 9, 13));
    }

    @Test
    void hospitalTimezoneIsConfigurable() {
        assertThat(new HospitalTimeConfiguration().hospitalClock("America/New_York").getZone())
                .isEqualTo(ZoneId.of("America/New_York"));
    }
}
