package com.example.hms;

import com.example.hms.config.RegistrationCodeInitializer;
import com.example.hms.domain.Doctor;
import com.example.hms.domain.Patient;
import com.example.hms.domain.enums.Gender;
import com.example.hms.dto.DoctorForm;
import com.example.hms.dto.PatientForm;
import com.example.hms.repository.DoctorRepository;
import com.example.hms.repository.PatientRepository;
import com.example.hms.service.DoctorService;
import com.example.hms.service.PatientService;
import jakarta.validation.ConstraintViolationException;
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
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DirectoryManagementTests {
    @Autowired private MockMvc mvc;
    @Autowired private PatientService patients;
    @Autowired private DoctorService doctors;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private RegistrationCodeInitializer codeInitializer;

    @ParameterizedTest
    @ValueSource(strings = {"/patients", "/patients/new", "/doctors", "/doctors/new"})
    void directoriesAndNewFormsRender(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("th:field"))));
    }

    @Test
    void patientRegistrationEditSearchAndDeleteFlow() throws Exception {
        long before = patientRepository.count();
        String location = mvc.perform(patientPost("/patients").param("id", "900000")
                        .param("patientCode", "FORGED").param("createdAt", "2000-01-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Patient registered successfully."))
                .andReturn().getResponse().getRedirectedUrl();
        Long id = idFrom(location);
        Patient created = patients.getPatient(id);
        String code = created.getPatientCode();
        assertThat(code).isEqualTo(String.format(Locale.ROOT, "PAT-%06d", id));
        assertThat(id).isNotEqualTo(900000L);
        assertThat(patientRepository.count()).isEqualTo(before + 1);
        assertThat(created.getAddress()).isEqualTo("Dhaka");

        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Amina Rahman")))
                .andExpect(content().string(containsString("Mother: 01800123456")));
        mvc.perform(get(location + "/edit")).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Amina\"")));
        mvc.perform(post(location + "/edit").param("firstName", "Amina").param("lastName", "Khan")
                        .param("phone", "01900123456").param("gender", "FEMALE")
                        .param("bloodGroup", "B-").param("dateOfBirth", "2000-01-15"))
                .andExpect(redirectedUrl(location));
        assertThat(patients.getPatient(id).getPatientCode()).isEqualTo(code);
        assertThat(patients.getPatient(id).getPhone()).isEqualTo("01900123456");
        mvc.perform(get("/patients").param("keyword", code.toLowerCase(Locale.ROOT)))
                .andExpect(content().string(containsString("Amina Khan")));

        mvc.perform(get(location + "/delete")).andExpect(status().isOk())
                .andExpect(content().string(containsString("This cannot be undone")));
        assertThat(patientRepository.existsById(id)).isTrue();
        mvc.perform(post(location + "/delete")).andExpect(redirectedUrl("/patients"));
        assertThat(patientRepository.existsById(id)).isFalse();
        mvc.perform(get(location)).andExpect(status().isNotFound());
    }

    @Test
    void doctorRegistrationUpdateAndDeletionPersistPracticeDetails() throws Exception {
        String location = mvc.perform(doctorPost("/doctors"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        Long id = idFrom(location);
        String code = doctors.getDoctor(id).getDoctorCode();
        assertThat(code).isEqualTo(String.format(Locale.ROOT, "DOC-%06d", id));
        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(content().string(containsString("MBBS, MD")));
        mvc.perform(get(location + "/edit")).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Cardiology\"")));
        mvc.perform(post(location + "/edit").param("firstName", "Karim").param("lastName", "Ahmed")
                        .param("phone", "01800123456").param("specialization", "Neurology")
                        .param("consultationFee", "750.50").param("_available", "on"))
                .andExpect(redirectedUrl(location));
        Doctor updated = doctors.getDoctor(id);
        assertThat(updated.isAvailable()).isFalse();
        assertThat(updated.getConsultationFee()).isEqualByComparingTo("750.50");
        assertThat(updated.getSpecialization()).isEqualTo("Neurology");
        assertThat(updated.getDoctorCode()).isEqualTo(code);
        mvc.perform(get(location + "/delete")).andExpect(status().isOk());
        assertThat(doctorRepository.existsById(id)).isTrue();
        mvc.perform(post(location + "/delete")).andExpect(redirectedUrl("/doctors"));
        mvc.perform(get(location)).andExpect(status().isNotFound());
    }

    @Test
    void invalidPatientCreateAndEditShowErrorsWithoutWriting() throws Exception {
        long before = patientRepository.count();
        mvc.perform(post("/patients").param("firstName", " ").param("lastName", "Rahman")
                        .param("email", "bad-email").param("gender", "FEMALE")
                        .param("dateOfBirth", LocalDate.now().plusDays(1).toString()).param("bloodGroup", "Z+"))
                .andExpect(status().isOk()).andExpect(view().name("patients/form"))
                .andExpect(model().attributeHasFieldErrors("form", "firstName", "phone", "email", "dateOfBirth", "bloodGroup"))
                .andExpect(content().string(containsString("value=\"Rahman\"")));
        assertThat(patientRepository.count()).isEqualTo(before);
        Patient saved = patients.createPatient(patientForm("Original"));
        mvc.perform(post("/patients/" + saved.getId() + "/edit").param("firstName", "Changed")
                        .param("lastName", "Rahman").param("phone", "valid").param("gender", "FEMALE")
                        .param("dateOfBirth", "not-a-date"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "dateOfBirth"))
                .andExpect(content().string(containsString("Enter a valid date")));
        assertThat(patients.getPatient(saved.getId()).getFirstName()).isEqualTo("Original");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "12.345", "10000000000", "abc", ""})
    void invalidDoctorFeesDoNotWrite(String fee) throws Exception {
        long before = doctorRepository.count();
        mvc.perform(post("/doctors").param("firstName", "Karim").param("lastName", "Ahmed")
                        .param("phone", "01800123456").param("specialization", "Cardiology")
                        .param("consultationFee", fee))
                .andExpect(status().isOk()).andExpect(view().name("doctors/form"))
                .andExpect(model().attributeHasFieldErrors("form", "consultationFee"));
        assertThat(doctorRepository.count()).isEqualTo(before);
    }

    @Test
    void patientSearchCombinesFiltersEscapesWildcardsAndPaginates() throws Exception {
        for (int i = 0; i < 11; i++) {
            patients.createPatient(patientForm("Searchable"));
        }
        PatientForm excluded = patientForm("Excluded");
        excluded.setBloodGroup("O-");
        patients.createPatient(excluded);
        var page = patients.search("searchable rahman", Gender.FEMALE, "A+", 0);
        assertThat(page.getTotalElements()).isEqualTo(11);
        assertThat(page.getContent()).hasSize(10);
        assertThat(patients.search("Searchable", Gender.FEMALE, "A+", 1).getContent()).hasSize(1);
        assertThat(patients.search("Searchable", Gender.MALE, "A+", 0)).isEmpty();
        assertThat(patients.search("%", null, null, 0)).isEmpty();
        mvc.perform(get("/patients").param("keyword", "Searchable").param("gender", "FEMALE").param("bloodGroup", "A+"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("page=1")))
                .andExpect(content().string(containsString("gender=FEMALE")))
                .andExpect(content().string(not(containsString("Excluded"))));
    }

    @Test
    void doctorSearchCombinesSpecializationAvailabilityAndContact() throws Exception {
        DoctorForm cardiology = doctorForm();
        Doctor active = doctors.createDoctor(cardiology);
        cardiology.setAvailable(false);
        cardiology.setFirstName("Unavailable");
        doctors.createDoctor(cardiology);
        DoctorForm neurology = doctorForm();
        neurology.setSpecialization("Neurology");
        doctors.createDoctor(neurology);
        assertThat(doctors.search("", "Cardiology", true, 0).getContent()).extracting(Doctor::getId).containsExactly(active.getId());
        assertThat(doctors.search("karim@example.com", "Cardiology", false, 0).getTotalElements()).isEqualTo(1);
        assertThat(doctors.search("_", "", null, 0)).isEmpty();
        assertThat(doctors.specializations()).containsExactly("Cardiology", "Neurology");
        mvc.perform(get("/doctors").param("specialization", "Cardiology").param("available", "true"))
                .andExpect(status().isOk()).andExpect(content().string(containsString(active.getDoctorCode())))
                .andExpect(content().string(not(containsString(">Unavailable</a>"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/patients/-1", "/patients/-1/edit", "/patients/-1/delete",
            "/doctors/-1", "/doctors/-1/edit", "/doctors/-1/delete"})
    void missingRecordsReturnFriendly404(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Record not found")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/patients/no-id", "/patients?gender=INVALID", "/doctors?available=INVALID", "/doctors?page=abc"})
    void malformedRequestParametersReturnFriendly400(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid request")));
    }

    @Test
    void directServiceCallsAlsoValidateForms() {
        PatientForm invalid = patientForm("Valid");
        invalid.setEmail("bad-email");
        assertThatThrownBy(() -> patients.createPatient(invalid)).isInstanceOf(ConstraintViolationException.class);
        DoctorForm invalidDoctor = doctorForm();
        invalidDoctor.setSpecialization(" ");
        assertThatThrownBy(() -> doctors.createDoctor(invalidDoctor)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void existingPhase3RecordsReceiveStableCodesWithoutBeingReplaced() {
        Patient patient = patientRepository.saveAndFlush(new Patient("Legacy", "Patient", "01700123456", null, Gender.NOT_SPECIFIED, null));
        Doctor doctor = doctorRepository.saveAndFlush(new Doctor("Legacy", "Doctor", "01800123456", null, "ENT", BigDecimal.ZERO));
        Long id = patient.getId();
        var createdAt = patient.getCreatedAt();
        codeInitializer.run(null);
        String patientCode = patient.getPatientCode();
        String doctorCode = doctor.getDoctorCode();
        codeInitializer.run(null);
        patientRepository.flush();
        assertThat(patient.getId()).isEqualTo(id);
        assertThat(patient.getCreatedAt()).isEqualTo(createdAt);
        assertThat(patient.getPatientCode()).isNotNull().isEqualTo(patientCode);
        assertThat(doctor.getDoctorCode()).isNotNull().isEqualTo(doctorCode);
    }

    @Test
    void storedTextIsEscapedInViews() throws Exception {
        Patient patient = patients.createPatient(patientForm("<script>alert(1)</script>"));
        mvc.perform(get("/patients/" + patient.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    private static Long idFrom(String location) {
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private static MockHttpServletRequestBuilder patientPost(String path) {
        return post(path).param("firstName", " Amina ").param("lastName", "Rahman")
                .param("phone", "01700123456").param("email", "amina@example.com")
                .param("gender", "FEMALE").param("dateOfBirth", "2000-01-15")
                .param("bloodGroup", "A+").param("address", "Dhaka")
                .param("emergencyContact", "Mother: 01800123456");
    }

    private static MockHttpServletRequestBuilder doctorPost(String path) {
        return post(path).param("firstName", "Karim").param("lastName", "Ahmed")
                .param("phone", "01800123456").param("email", "karim@example.com")
                .param("specialization", "Cardiology").param("qualification", "MBBS, MD")
                .param("consultationFee", "500.00").param("available", "true");
    }

    private static PatientForm patientForm(String firstName) {
        PatientForm form = new PatientForm();
        form.setFirstName(firstName);
        form.setLastName("Rahman");
        form.setPhone("01700123456");
        form.setGender(Gender.FEMALE);
        form.setBloodGroup("A+");
        return form;
    }

    private static DoctorForm doctorForm() {
        DoctorForm form = new DoctorForm();
        form.setFirstName("Karim");
        form.setLastName("Ahmed");
        form.setPhone("01800123456");
        form.setEmail("karim@example.com");
        form.setSpecialization("Cardiology");
        form.setConsultationFee(new BigDecimal("500.00"));
        return form;
    }
}
