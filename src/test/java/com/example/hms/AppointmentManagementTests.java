package com.example.hms;

import com.example.hms.domain.*;
import com.example.hms.domain.enums.*;
import com.example.hms.dto.AppointmentForm;
import com.example.hms.exception.*;
import com.example.hms.repository.*;
import com.example.hms.service.AppointmentService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppointmentManagementTests {
    @Autowired private MockMvc mvc;
    @Autowired private AppointmentService service;
    @Autowired private AppointmentRepository appointments;
    @Autowired private PatientRepository patients;
    @Autowired private DoctorRepository doctors;
    private Patient patient;
    private Doctor doctor;
    private static final LocalDate DATE = LocalDate.of(2030, 6, 15);

    @BeforeEach
    void registerPeople() {
        patient = patients.saveAndFlush(new Patient("Amina", "Rahman", "01700123456", null, Gender.FEMALE, null));
        patient.assignPatientCode();
        doctor = doctors.saveAndFlush(new Doctor("Karim", "Ahmed", "01800123456", null, "Cardiology", new BigDecimal("500")));
        doctor.assignDoctorCode();
        doctors.flush();
    }

    @Test
    void listAndPreselectedFormRender() throws Exception {
        mvc.perform(get("/appointments")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No appointments found")));
        mvc.perform(get("/appointments/new").param("patientId", patient.getId().toString()).param("doctorId", doctor.getId().toString()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("form", "patients", "doctors"))
                .andExpect(content().string(containsString("Amina Rahman")))
                .andExpect(content().string(not(containsString("th:field"))));
    }

    @Test
    void createDetailsEditAndConfirmFlow() throws Exception {
        String location = mvc.perform(bookingPost("/appointments", form("10:00"))
                        .param("status", "COMPLETED").param("appointmentCode", "FORGED"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        Long id = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
        Appointment appointment = service.getAppointment(id);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(appointment.getAppointmentCode()).startsWith("APT-").isNotEqualTo("FORGED");
        String code = appointment.getAppointmentCode();
        mvc.perform(get(location)).andExpect(status().isOk()).andExpect(content().string(containsString("Karim Ahmed")));
        mvc.perform(get(location + "/edit")).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"10:00:00\"")));
        mvc.perform(get(location + "/confirm")).andExpect(status().isOk());
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        mvc.perform(post(location + "/confirm").param("version", appointment.getVersion().toString()))
                .andExpect(redirectedUrl(location));
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        AppointmentForm edit = AppointmentForm.from(appointment);
        edit.setNotes("Follow-up notes");
        mvc.perform(bookingPost(location + "/edit", edit)).andExpect(redirectedUrl(location));
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getAppointmentCode()).isEqualTo(code);
        assertThat(appointment.getNotes()).isEqualTo("Follow-up notes");
        edit = AppointmentForm.from(appointment);
        edit.setAppointmentTime(LocalTime.of(11, 0));
        service.updateAppointment(id, edit);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(service.isDoctorAvailable(doctor.getId(), DATE, LocalTime.of(10, 0))).isTrue();
    }

    @Test
    void occupiedSlotIsRejectedAndOtherDoctorOrDateIsAllowed() throws Exception {
        service.createAppointment(form("10:00"));
        long count = appointments.count();
        mvc.perform(bookingPost("/appointments", form("10:00")))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "appointmentTime"))
                .andExpect(content().string(containsString("already has an appointment")));
        assertThat(appointments.count()).isEqualTo(count);
        assertThat(service.isDoctorAvailable(doctor.getId(), DATE, LocalTime.of(10, 0))).isFalse();
        Doctor other = doctors.saveAndFlush(new Doctor("Other", "Doctor", "01900123456", null, "ENT", BigDecimal.ZERO));
        AppointmentForm differentDoctor = form("10:00");
        differentDoctor.setDoctorId(other.getId());
        service.createAppointment(differentDoctor);
        AppointmentForm differentDate = form("10:00");
        differentDate.setAppointmentDate(DATE.plusDays(1));
        service.createAppointment(differentDate);
        assertThat(appointments.count()).isEqualTo(count + 2);
    }

    @Test
    void conflictingUpdateLeavesOriginalBookingUntouched() throws Exception {
        service.createAppointment(form("10:00"));
        Appointment other = service.createAppointment(form("11:00"));
        AppointmentForm edit = AppointmentForm.from(other);
        edit.setAppointmentTime(LocalTime.of(10, 0));
        edit.setNotes("Should not be saved");
        mvc.perform(bookingPost("/appointments/" + other.getId() + "/edit", edit))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "appointmentTime"));
        assertThat(other.getAppointmentTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(other.getNotes()).isNull();
    }

    @Test
    void cancellationFreesSlotAndIsTerminal() throws Exception {
        Appointment appointment = service.createAppointment(form("10:00"));
        String path = "/appointments/" + appointment.getId();
        mvc.perform(get(path + "/cancel")).andExpect(status().isOk());
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        mvc.perform(post(path + "/cancel").param("version", appointment.getVersion().toString()))
                .andExpect(redirectedUrl(path));
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(service.isDoctorAvailable(doctor.getId(), DATE, LocalTime.of(10, 0))).isTrue();
        service.createAppointment(form("10:00"));
        mvc.perform(get(path + "/edit")).andExpect(status().isConflict());
        mvc.perform(bookingPost(path + "/edit", AppointmentForm.from(appointment))).andExpect(status().isConflict());
        mvc.perform(post(path + "/complete").param("version", appointment.getVersion().toString())).andExpect(status().isConflict());
        mvc.perform(get(path)).andExpect(content().string(containsString("read-only")));
    }

    @Test
    void completedAppointmentsAreImmutableAndStillOccupyTheirSlot() throws Exception {
        Appointment appointment = service.createAppointment(form("10:00"));
        String path = "/appointments/" + appointment.getId();
        mvc.perform(get(path + "/complete")).andExpect(status().isOk());
        mvc.perform(post(path + "/complete").param("version", appointment.getVersion().toString())).andExpect(redirectedUrl(path));
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThatThrownBy(() -> service.createAppointment(form("10:00"))).isInstanceOf(AppointmentConflictException.class);
        mvc.perform(get(path + "/edit")).andExpect(status().isConflict());
        mvc.perform(post(path + "/cancel").param("version", appointment.getVersion().toString())).andExpect(status().isConflict());
        assertThatThrownBy(() -> appointment.updateSchedule(patient, doctor, DATE, LocalTime.NOON, "changed", null))
                .isInstanceOf(AppointmentStateException.class);
    }

    @Test
    void unavailableDoctorRejectsNewBookingsButAllowsExistingNotes() throws Exception {
        Appointment existing = service.createAppointment(form("10:00"));
        doctor.markUnavailable();
        doctors.flush();
        mvc.perform(bookingPost("/appointments", form("12:00"))).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "doctorId"));
        assertThat(service.isDoctorAvailable(doctor.getId(), DATE, LocalTime.NOON)).isFalse();
        AppointmentForm edit = AppointmentForm.from(existing);
        edit.setNotes("Keep the existing booking");
        service.updateAppointment(existing.getId(), edit);
        assertThat(existing.getNotes()).isEqualTo("Keep the existing booking");
        AppointmentForm moved = AppointmentForm.from(existing);
        moved.setAppointmentTime(LocalTime.NOON);
        assertThatThrownBy(() -> service.updateAppointment(existing.getId(), moved)).isInstanceOf(AppointmentValidationException.class);
    }

    @Test
    void staleEditsAndStatusActionsAreRejected() throws Exception {
        Appointment appointment = service.createAppointment(form("10:00"));
        AppointmentForm stale = AppointmentForm.from(appointment);
        Long oldVersion = appointment.getVersion();
        service.confirmAppointment(appointment.getId(), oldVersion);
        String path = "/appointments/" + appointment.getId();
        mvc.perform(bookingPost(path + "/edit", stale)).andExpect(status().isConflict())
                .andExpect(content().string(containsString("has changed")));
        mvc.perform(post(path + "/cancel").param("version", oldVersion.toString())).andExpect(status().isConflict());
        mvc.perform(post(path + "/cancel")).andExpect(status().isBadRequest());
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void requiredAndMalformedInputDoNotCreateRecords() throws Exception {
        mvc.perform(post("/appointments")).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "patientId", "doctorId", "appointmentDate", "appointmentTime"));
        mvc.perform(post("/appointments").param("patientId", "abc").param("doctorId", "abc")
                        .param("appointmentDate", "2030-02-31").param("appointmentTime", "25:99"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "patientId", "doctorId", "appointmentDate", "appointmentTime"));
        assertThat(appointments.count()).isZero();
        assertThatThrownBy(() -> service.createAppointment(new AppointmentForm())).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void missingSelectedPeopleAreFieldErrors() throws Exception {
        AppointmentForm form = form("10:00");
        form.setPatientId(Long.MAX_VALUE);
        mvc.perform(bookingPost("/appointments", form)).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "patientId"));
        form.setPatientId(patient.getId());
        form.setDoctorId(Long.MAX_VALUE);
        mvc.perform(bookingPost("/appointments", form)).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "doctorId"));
        assertThat(appointments.count()).isZero();
    }

    @Test
    void secondsAndOversizedTextAreRejected() throws Exception {
        AppointmentForm form = form("10:00:01");
        form.setReason("a".repeat(1001));
        form.setNotes("a".repeat(2001));
        mvc.perform(bookingPost("/appointments", form)).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "minutePrecision", "reason", "notes"));
        assertThat(appointments.count()).isZero();
    }

    @Test
    void filtersPaginationAndHistoryLinksWork() throws Exception {
        for (int hour = 0; hour < 11; hour++) {
            service.createAppointment(form(String.format("%02d:00", hour)));
        }
        AppointmentForm anotherDay = form("15:00");
        anotherDay.setAppointmentDate(DATE.plusDays(1));
        service.createAppointment(anotherDay);
        var page = service.search(DATE, patient.getId(), doctor.getId(), AppointmentStatus.SCHEDULED, 0);
        assertThat(page.getTotalElements()).isEqualTo(11);
        assertThat(page.getContent()).hasSize(10);
        assertThat(service.search(DATE, patient.getId(), doctor.getId(), AppointmentStatus.SCHEDULED, 1).getContent()).hasSize(1);
        assertThat(service.search(DATE, patient.getId(), doctor.getId(), AppointmentStatus.CANCELLED, 0)).isEmpty();
        mvc.perform(get("/appointments").param("date", DATE.toString()).param("patientId", patient.getId().toString())
                        .param("doctorId", doctor.getId().toString()).param("status", "SCHEDULED"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("page=1")))
                .andExpect(content().string(containsString("status=SCHEDULED")))
                .andExpect(content().string(containsString("date=2030-06-15")));
        mvc.perform(get("/patients/" + patient.getId())).andExpect(content().string(containsString("/appointments?patientId=")));
        mvc.perform(get("/doctors/" + doctor.getId())).andExpect(content().string(containsString("/appointments?doctorId=")));
    }

    @Test
    void historyProtectsPatientAndDoctorFromDeletionEvenAfterCancellation() throws Exception {
        Appointment appointment = service.createAppointment(form("10:00"));
        service.cancelAppointment(appointment.getId(), appointment.getVersion());
        mvc.perform(post("/patients/" + patient.getId() + "/delete")).andExpect(status().isConflict());
        mvc.perform(post("/doctors/" + doctor.getId() + "/delete")).andExpect(status().isConflict());
        assertThat(patients.existsById(patient.getId())).isTrue();
        assertThat(doctors.existsById(doctor.getId())).isTrue();
        assertThat(appointments.existsById(appointment.getId())).isTrue();
    }

    @Test
    void notesAreEscaped() throws Exception {
        AppointmentForm form = form("10:00");
        form.setNotes("<script>alert(1)</script>");
        Appointment appointment = service.createAppointment(form);
        mvc.perform(get("/appointments/" + appointment.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/appointments/-1", "/appointments/-1/edit", "/appointments/-1/cancel", "/appointments/-1/complete"})
    void missingAppointmentsReturn404(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/appointments?date=bad", "/appointments?status=bad", "/appointments?doctorId=bad", "/appointments?page=bad"})
    void malformedFiltersReturn400(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest());
    }

    private AppointmentForm form(String time) {
        AppointmentForm form = new AppointmentForm();
        form.setPatientId(patient.getId());
        form.setDoctorId(doctor.getId());
        form.setAppointmentDate(DATE);
        form.setAppointmentTime(LocalTime.parse(time));
        form.setReason("Checkup");
        return form;
    }

    private MockHttpServletRequestBuilder bookingPost(String path, AppointmentForm form) {
        var request = post(path).param("patientId", form.getPatientId().toString())
                .param("doctorId", form.getDoctorId().toString()).param("appointmentDate", form.getAppointmentDate().toString())
                .param("appointmentTime", form.getAppointmentTime().toString());
        if (form.getVersion() != null) { request.param("version", form.getVersion().toString()); }
        if (form.getReason() != null) { request.param("reason", form.getReason()); }
        if (form.getNotes() != null) { request.param("notes", form.getNotes()); }
        return request;
    }
}
