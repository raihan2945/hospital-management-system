package com.example.hms;

import com.example.hms.dto.*;
import com.example.hms.repository.*;
import com.example.hms.service.*;
import jakarta.servlet.RequestDispatcher;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ValidationAndErrorTests.ProbeConfiguration.class)
class ValidationAndErrorTests {
    @Autowired MockMvc mvc;
    @Autowired BillingService billing;
    @Autowired PatientService patientService;
    @Autowired DoctorService doctorService;
    @Autowired AppointmentService appointmentService;
    @Autowired BillRepository bills;
    @Autowired PatientRepository patients;

    @TestConfiguration
    static class ProbeConfiguration {
        @Bean ErrorProbe errorProbe() { return new ErrorProbe(); }
    }
    @Controller
    static class ErrorProbe {
        @GetMapping("/__test/errors/{kind}")
        String error(@PathVariable String kind) {
            throw switch (kind) {
                case "database" -> new DataAccessResourceFailureException("SECRET_DATABASE_DETAILS");
                case "lock" -> new CannotAcquireLockException("SECRET_LOCK_DETAILS");
                case "validation" -> new ConstraintViolationException("SECRET_INVALID_VALUE", Set.of());
                case "integrity" -> new DataIntegrityViolationException("SECRET_SQL_CONSTRAINT");
                case "commit-validation" -> new TransactionSystemException("SECRET_ROLLBACK",
                        new ConstraintViolationException("SECRET_ENTITY_VIOLATION", Set.of()));
                case "commit-failure" -> new TransactionSystemException("SECRET_ROLLBACK", new IllegalStateException("SECRET_COMMIT_FAILURE"));
                case "status" -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SECRET_INTERNAL_REASON");
                default -> new IllegalStateException("SECRET_INTERNAL_FAILURE");
            };
        }
    }

    @ParameterizedTest
    @CsvSource({"database,503", "lock,409", "validation,400", "integrity,409", "status,404", "unexpected,500",
            "commit-validation,400", "commit-failure,500"})
    void exceptionsReturnSafePagesAndCorrectStatus(String kind, int code) throws Exception {
        mvc.perform(get("/__test/errors/" + kind).param("trace", "true"))
                .andExpect(status().is(code)).andExpect(view().name("error/message"))
                .andExpect(model().attribute("statusCode", code))
                .andExpect(content().string(not(containsString("SECRET_"))))
                .andExpect(content().string(not(containsString("java.lang."))))
                .andExpect(content().string(containsString("/dashboard")));
    }

    @Test
    void unexpectedFailureHasSupportReferenceAndServletFallbackHidesRawDetails() throws Exception {
        mvc.perform(get("/__test/errors/unexpected")).andExpect(status().isInternalServerError())
                .andExpect(model().attributeExists("reference"));
        mvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "SECRET_SERVLET_MESSAGE")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new RuntimeException("SECRET_EXCEPTION")))
                .andExpect(status().isInternalServerError()).andExpect(view().name("error/message"))
                .andExpect(content().string(not(containsString("SECRET_"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/page-that-does-not-exist", "/css/not-found.css", "/error"})
    void unmappedRoutesAndAssetsHaveFriendly404(String route) throws Exception {
        mvc.perform(get(route)).andExpect(status().isNotFound()).andExpect(view().name("error/message"))
                .andExpect(content().string(containsString("Page not found")));
    }

    @Test
    void unsupportedMethodKeepsAllowHeader() throws Exception {
        mvc.perform(put("/patients")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(view().name("error/message"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/patients", "/doctors", "/appointments", "/billing"})
    void invalidPaginationCannotOverflowDatabaseOffset(String route) throws Exception {
        for (String page : new String[]{"-1", "2147483647"}) {
            mvc.perform(get(route).param("page", page)).andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("valid page number")));
        }
    }

    @Test
    void billFieldsHaveReadableMessagesAndKeepOtherInput() throws Exception {
        mvc.perform(post("/billing").param("patientId", "1").param("serviceCharge", "bad")
                        .param("medicineCharge", "-1").param("otherCharge", "1.001").param("consultationFee", "123.45"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "serviceCharge", "medicineCharge", "otherCharge"))
                .andExpect(content().string(containsString("Enter a valid service charge")))
                .andExpect(content().string(containsString("Medicine charge cannot be negative")))
                .andExpect(content().string(containsString("value=\"123.45\"")));
        assertThat(bills.count()).isZero();
    }

    @Test
    void discountIsValidatedAtFormAndServiceBoundariesBeforeAnyWrite() throws Exception {
        mvc.perform(post("/billing").param("patientId", "1").param("consultationFee", "10").param("discount", "11"))
                .andExpect(model().attributeHasFieldErrors("form", "discount"))
                .andExpect(content().string(containsString("Discount cannot exceed the subtotal")));
        CreateBillForm form = new CreateBillForm(); form.setPatientId(1L); form.setDiscount(BigDecimal.ONE);
        assertThatThrownBy(() -> billing.createBill(form)).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Discount cannot exceed the subtotal");
        assertThat(bills.count()).isZero();
    }

    @Test
    void nullFormsCannotBypassServiceValidation() {
        assertThatThrownBy(() -> patientService.createPatient(null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> doctorService.createDoctor(null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> appointmentService.createAppointment(null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> billing.createBill(null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> billing.recordPayment(1L, null)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void sharedContactDetailsAreAllowedWhileGeneratedPatientCodesRemainUnique() {
        PatientForm form = new PatientForm(); form.setFirstName("Shared"); form.setLastName("Contact");
        form.setPhone("01700123456"); form.setEmail("family@example.com");
        var first = patientService.createPatient(form);
        var second = patientService.createPatient(form);
        assertThat(first.getPatientCode()).isNotEqualTo(second.getPatientCode());
        assertThat(patients.count()).isEqualTo(2);
    }
}
