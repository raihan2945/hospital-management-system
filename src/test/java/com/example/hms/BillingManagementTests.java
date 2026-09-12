package com.example.hms;

import com.example.hms.domain.*;
import com.example.hms.domain.enums.*;
import com.example.hms.dto.*;
import com.example.hms.exception.*;
import com.example.hms.repository.*;
import com.example.hms.service.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BillingManagementTests {
    @Autowired MockMvc mvc;
    @Autowired BillingService service;
    @Autowired AppointmentService appointmentService;
    @Autowired PatientService patientService;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired BillRepository bills;
    @Autowired PaymentRepository payments;
    private Patient patient;
    private Doctor doctor;

    @BeforeEach
    void people() {
        patient = patients.saveAndFlush(new Patient("Amina", "Rahman", "01700123456", null, Gender.FEMALE, null));
        patient.assignPatientCode();
        doctor = doctors.saveAndFlush(new Doctor("Karim", "Ahmed", "01800123456", null, "ENT", new BigDecimal("500.00")));
        doctor.assignDoctorCode();
    }

    @Test
    void createInvoiceComputesTotalsAndIgnoresForgedFields() throws Exception {
        mvc.perform(get("/billing")).andExpect(status().isOk()).andExpect(content().string(containsString("No invoices found")));
        mvc.perform(get("/billing/new").param("patientId", patient.getId().toString())).andExpect(status().isOk());
        String location = mvc.perform(billPost().param("totalAmount", "1").param("paidAmount", "800")
                        .param("paymentStatus", "PAID").param("invoiceNumber", "FORGED"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        Bill bill = service.getBill(Long.valueOf(location.substring(location.lastIndexOf('/') + 1)));
        assertThat(bill.getSubtotal()).isEqualByComparingTo("875.50");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("800.00");
        assertThat(bill.getPaidAmount()).isEqualByComparingTo("0.00");
        assertThat(bill.getDueAmount()).isEqualByComparingTo("800.00");
        assertThat(bill.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(bill.getInvoiceNumber()).startsWith("INV-");
        mvc.perform(get(location)).andExpect(status().isOk()).andExpect(content().string(containsString("800.00")))
                .andExpect(content().string(containsString("Print invoice"))).andExpect(content().string(containsString("No payments recorded")));
        mvc.perform(get(location + "/payment")).andExpect(status().isOk()).andExpect(content().string(containsString("Bank transfer")));
        mvc.perform(get("/billing")).andExpect(status().isOk()).andExpect(content().string(containsString(bill.getInvoiceNumber())));
        mvc.perform(get("/patients/" + patient.getId())).andExpect(content().string(containsString("View billing history")));
    }

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    void eachProcessorRecordsPartialAndFinalPayments(PaymentMethod method) throws Exception {
        Bill bill = service.createBill(form());
        String invoice = bill.getInvoiceNumber();
        mvc.perform(paymentPost(bill, "300.00", method).param("transactionReference", "  RECEIPT-123  "))
                .andExpect(redirectedUrl("/billing/" + bill.getId()));
        assertThat(bill.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(bill.getDueAmount()).isEqualByComparingTo("500");
        Payment payment = service.paymentHistory(bill.getId()).getFirst();
        assertThat(payment.getPaymentMethod()).isEqualTo(method);
        assertThat(payment.getTransactionReference()).isEqualTo("RECEIPT-123");
        assertThat(payment.getPaymentDate()).isNotNull();
        mvc.perform(paymentPost(bill, "500.00", PaymentMethod.CASH)).andExpect(status().is3xxRedirection());
        assertThat(bill.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(bill.getDueAmount()).isEqualByComparingTo("0");
        assertThat(bill.getInvoiceNumber()).isEqualTo(invoice);
        assertThat(service.paymentHistory(bill.getId())).hasSize(2);
        mvc.perform(get("/billing/" + bill.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("RECEIPT-123")))
                .andExpect(content().string(not(containsString(">Record payment</a>"))));
        mvc.perform(get("/billing/" + bill.getId() + "/payment")).andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.001", "10000000000", "abc", ""})
    void invalidChargesKeepFormAndDoNotWrite(String value) throws Exception {
        long before = bills.count();
        mvc.perform(post("/billing").param("patientId", patient.getId().toString()).param("consultationFee", value))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "consultationFee"));
        assertThat(bills.count()).isEqualTo(before);
    }

    @Test
    void excessiveDiscountAndMissingPatientAreRejected() throws Exception {
        mvc.perform(post("/billing").param("patientId", patient.getId().toString()).param("discount", "1"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "discount"));
        mvc.perform(post("/billing")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "patientId"));
        mvc.perform(post("/billing").param("patientId", "999999999"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "patientId"));
        assertThat(bills.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "800.01", "1.001", "100000000000", "bad", ""})
    void invalidPaymentsDoNotChangeBalanceOrHistory(String amount) throws Exception {
        Bill bill = service.createBill(form());
        mvc.perform(paymentPost(bill, amount, PaymentMethod.CASH)).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("paymentForm", "amount"));
        assertThat(bill.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(service.paymentHistory(bill.getId())).isEmpty();
    }

    @Test
    void repeatedPaymentAndStaleFormAreRejected() throws Exception {
        Bill bill = service.createBill(form());
        String version = bill.getVersion().toString();
        mvc.perform(paymentPost(bill, "100", PaymentMethod.CASH)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/billing/" + bill.getId() + "/payment").param("version", version)
                        .param("amount", "100").param("paymentMethod", "CASH"))
                .andExpect(status().isConflict()).andExpect(content().string(containsString("already submitted")));
        assertThat(service.paymentHistory(bill.getId())).hasSize(1);
        assertThat(bill.getPaidAmount()).isEqualByComparingTo("100");
    }

    @Test
    void paymentBindingRequiresMethodAndVersionAndEscapesReference() throws Exception {
        Bill bill = service.createBill(form());
        mvc.perform(post("/billing/" + bill.getId() + "/payment").param("amount", "10"))
                .andExpect(model().attributeHasFieldErrors("paymentForm", "paymentMethod", "version"));
        mvc.perform(paymentPost(bill, "10", PaymentMethod.CARD).param("transactionReference", "x".repeat(101)))
                .andExpect(model().attributeHasFieldErrors("paymentForm", "transactionReference"));
        mvc.perform(paymentPost(bill, "10", PaymentMethod.CARD).param("transactionReference", "<script>alert(1)</script>"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/billing/" + bill.getId())).andExpect(content().string(containsString("&lt;script&gt;")));
    }

    @Test
    void appointmentPrefillsFeeAndAllowsOnlyOneMatchingBill() throws Exception {
        Appointment appointment = appointmentService.createAppointment(appointmentForm());
        mvc.perform(get("/billing/new").param("appointmentId", appointment.getId().toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("500.00")));
        CreateBillForm form = form();
        form.setAppointmentId(appointment.getId());
        Bill bill = service.createBill(form);
        mvc.perform(get("/appointments/" + appointment.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("View invoice " + bill.getInvoiceNumber())));
        assertThatThrownBy(() -> service.createBill(form)).isInstanceOf(BillingValidationException.class).hasMessageContaining("already has an invoice");
        assertThatThrownBy(() -> appointmentService.updateAppointment(appointment.getId(), AppointmentForm.from(appointment)))
                .isInstanceOf(AppointmentStateException.class).hasMessageContaining("invoice");
        assertThatThrownBy(() -> appointmentService.cancelAppointment(appointment.getId(), appointment.getVersion()))
                .isInstanceOf(AppointmentStateException.class);
        assertThat(service.appointmentOptions()).extracting(Appointment::getId).doesNotContain(appointment.getId());
        assertThat(bills.count()).isEqualTo(1);
    }

    @Test
    void billedAppointmentCanStillBeCompleted() {
        Appointment appointment = appointmentService.createAppointment(appointmentForm());
        CreateBillForm form = form();
        form.setAppointmentId(appointment.getId());
        service.createBill(form);
        appointmentService.confirmAppointment(appointment.getId(), appointment.getVersion());
        appointmentService.completeAppointment(appointment.getId(), appointment.getVersion());
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void mismatchedAndCancelledAppointmentsCannotBeBilled() throws Exception {
        Appointment appointment = appointmentService.createAppointment(appointmentForm());
        Patient other = patients.saveAndFlush(new Patient("Other", "Patient", "01700123457", null, Gender.OTHER, null));
        mvc.perform(post("/billing").param("patientId", other.getId().toString()).param("appointmentId", appointment.getId().toString()))
                .andExpect(model().attributeHasFieldErrors("form", "appointmentId"));
        appointmentService.cancelAppointment(appointment.getId(), appointment.getVersion());
        mvc.perform(post("/billing").param("patientId", patient.getId().toString()).param("appointmentId", appointment.getId().toString()))
                .andExpect(model().attributeHasFieldErrors("form", "appointmentId"));
        assertThat(bills.count()).isZero();
    }

    @Test
    void zeroTotalIsPaidAndHistoricalPatientDetailsArePreserved() {
        CreateBillForm form = form();
        form.setDiscount(new BigDecimal("875.50"));
        Bill bill = service.createBill(form);
        assertThat(bill.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(bill.getDueAmount()).isEqualByComparingTo("0");
        patient.rename("Changed", "Name");
        assertThat(service.getBill(bill.getId()).getPatientName()).isEqualTo("Amina Rahman");
        assertThatThrownBy(() -> patientService.deletePatient(patient.getId())).isInstanceOf(ReferencedRecordException.class)
                .hasMessageContaining("billing history");
    }

    @Test
    void serviceValidationCannotBeBypassed() {
        CreateBillForm form = form();
        form.setOtherCharge(new BigDecimal("-1"));
        assertThatThrownBy(() -> service.createBill(form)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void filtersPaginationAndErrorPagesWork() throws Exception {
        for (int i = 0; i < 11; i++) { service.createBill(form()); }
        CreateBillForm zero = form(); zero.setDiscount(new BigDecimal("875.50")); service.createBill(zero);
        assertThat(service.search(patient.getId(), null, PaymentStatus.UNPAID, 0).getTotalElements()).isEqualTo(11);
        mvc.perform(get("/billing").param("patientId", patient.getId().toString()).param("status", "UNPAID"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("page=1")))
                .andExpect(content().string(containsString("status=UNPAID")));
        assertThat(service.search(patient.getId(), null, PaymentStatus.UNPAID, 1).getNumberOfElements()).isEqualTo(1);
        assertThat(service.search(patient.getId(), 999999L, null, 0)).isEmpty();
        mvc.perform(get("/billing/99999999")).andExpect(status().isNotFound());
        mvc.perform(get("/billing/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/billing").param("status", "BAD")).andExpect(status().isBadRequest());
        mvc.perform(get("/billing/99999999/payment")).andExpect(status().isNotFound());
    }

    private CreateBillForm form() {
        CreateBillForm form = new CreateBillForm();
        form.setPatientId(patient.getId());
        form.setConsultationFee(new BigDecimal("500"));
        form.setServiceCharge(new BigDecimal("200"));
        form.setMedicineCharge(new BigDecimal("150.50"));
        form.setOtherCharge(new BigDecimal("25"));
        form.setDiscount(new BigDecimal("75.50"));
        return form;
    }
    private MockHttpServletRequestBuilder billPost() {
        return post("/billing").param("patientId", patient.getId().toString()).param("consultationFee", "500")
                .param("serviceCharge", "200").param("medicineCharge", "150.50").param("otherCharge", "25").param("discount", "75.50");
    }
    private MockHttpServletRequestBuilder paymentPost(Bill bill, String amount, PaymentMethod method) {
        return post("/billing/" + bill.getId() + "/payment").param("amount", amount)
                .param("paymentMethod", method.name()).param("version", bill.getVersion().toString());
    }
    private AppointmentForm appointmentForm() {
        AppointmentForm form = new AppointmentForm(); form.setPatientId(patient.getId()); form.setDoctorId(doctor.getId());
        form.setAppointmentDate(LocalDate.of(2030, 7, 15)); form.setAppointmentTime(LocalTime.NOON); return form;
    }
}
