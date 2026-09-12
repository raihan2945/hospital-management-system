package com.example.hms;

import com.example.hms.domain.*;
import com.example.hms.domain.enums.*;
import com.example.hms.dto.CreateBillForm;
import com.example.hms.repository.*;
import com.example.hms.service.BillingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserInterfaceTests {
    @Autowired MockMvc mvc;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired AppointmentRepository appointments;
    @Autowired BillingService billing;

    @ParameterizedTest
    @ValueSource(strings = {"/dashboard", "/patients", "/doctors", "/appointments", "/billing", "/patients/new", "/doctors/new", "/appointments/new", "/billing/new"})
    void pagesShareAccessibleSidebarAndLocalEnhancements(String route) throws Exception {
        mvc.perform(get(route)).andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Main navigation\"")))
                .andExpect(content().string(containsString("aria-label=\"Open navigation\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("/vendor/bootstrap-icons/bootstrap-icons.svg#")))
                .andExpect(content().string(containsString("/js/app.js")))
                .andExpect(content().string(containsString("id=\"confirmation-modal\"")));
    }

    @Test
    void confirmationGetIsReadOnlyAndRetainsARealPostFallback() throws Exception {
        Patient patient = patients.saveAndFlush(new Patient("Confirmation", "Patient", "01700123456", null, Gender.OTHER, null));
        mvc.perform(get("/patients/" + patient.getId())).andExpect(content().string(containsString("data-confirm")));
        mvc.perform(get("/patients/" + patient.getId() + "/delete")).andExpect(status().isOk())
                .andExpect(content().string(containsString("data-confirmation-panel")))
                .andExpect(content().string(containsString("method=\"post\"")))
                .andExpect(content().string(containsString("data-confirm-cancel")));
        assertThat(patients.existsById(patient.getId())).isTrue();
    }

    @Test
    void billedAppointmentsDoNotAdvertiseUnavailableEditAction() throws Exception {
        Patient patient = patients.saveAndFlush(new Patient("Billed", "Patient", "01700123456", null, Gender.OTHER, null));
        Doctor doctor = doctors.saveAndFlush(new Doctor("Billed", "Doctor", "01800123456", null, "ENT", BigDecimal.TEN));
        Appointment appointment = appointments.saveAndFlush(new Appointment(patient, doctor, LocalDate.now(), LocalTime.NOON, null, null));
        CreateBillForm form = new CreateBillForm(); form.setPatientId(patient.getId()); form.setAppointmentId(appointment.getId()); form.setConsultationFee(BigDecimal.TEN);
        Bill bill = billing.createBill(form);
        mvc.perform(get("/appointments")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/appointments/" + appointment.getId() + "/edit"))))
                .andExpect(content().string(containsString("role=\"region\"")));
        mvc.perform(get("/billing/" + bill.getId())).andExpect(content().string(containsString("payment-unpaid")));
    }

    @Test
    void errorsRetainInputAndSuccessCanBeDismissed() throws Exception {
        mvc.perform(post("/patients").param("firstName", "Preserved"))
                .andExpect(model().attributeHasErrors("form"))
                .andExpect(content().string(containsString("data-error-summary")))
                .andExpect(content().string(containsString("value=\"Preserved\"")));
        mvc.perform(get("/patients").flashAttr("success", "Patient saved."))
                .andExpect(content().string(containsString("Patient saved.")))
                .andExpect(content().string(containsString("Dismiss success message")));
    }
}
