package com.example.hms;

import com.example.hms.dto.PatientForm;
import com.example.hms.repository.PatientRepository;
import com.example.hms.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:hms_date_validation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(HospitalDateValidationTests.FixedHospitalClock.class)
class HospitalDateValidationTests {
    @Autowired PatientService service;
    @Autowired PatientRepository patients;
    @Autowired MockMvc mvc;

    @TestConfiguration
    static class FixedHospitalClock {
        @Bean @Primary Clock validationClock() {
            return Clock.fixed(Instant.parse("2099-01-01T18:00:00Z"), ZoneId.of("Asia/Dhaka"));
        }
    }

    @Test
    void newbornTodayIsAcceptedByFormDomainAndPersistenceDespiteUtcBeingYesterday() throws Exception {
        PatientForm form = new PatientForm(); form.setFirstName("Newborn"); form.setLastName("Patient");
        form.setPhone("01700123456"); form.setDateOfBirth(LocalDate.of(2099, 1, 2));
        var patient = service.createPatient(form);
        patients.flush();
        assertThat(patient.getDateOfBirth()).isEqualTo(LocalDate.of(2099, 1, 2));
        mvc.perform(get("/patients/new")).andExpect(status().isOk())
                .andExpect(content().string(containsString("max=\"2099-01-02\"")));
        mvc.perform(post("/patients").param("firstName", "Newborn").param("lastName", "Web")
                        .param("phone", "01700123456").param("dateOfBirth", "2099-01-02"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void tomorrowIsStillRejected() throws Exception {
        mvc.perform(post("/patients").param("firstName", "Future").param("lastName", "Patient")
                        .param("phone", "01700123456").param("dateOfBirth", "2099-01-03"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "dateOfBirth"));
        assertThat(patients.count()).isZero();
    }
}
