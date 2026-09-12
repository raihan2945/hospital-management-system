package com.example.hms.config;

import com.example.hms.domain.Appointment;
import com.example.hms.domain.Bill;
import com.example.hms.domain.Doctor;
import com.example.hms.domain.Patient;
import com.example.hms.domain.enums.AppointmentStatus;
import com.example.hms.domain.enums.Gender;
import com.example.hms.domain.enums.PaymentMethod;
import com.example.hms.dto.AppointmentForm;
import com.example.hms.dto.CreateBillForm;
import com.example.hms.dto.DoctorForm;
import com.example.hms.dto.PatientForm;
import com.example.hms.dto.PaymentForm;
import com.example.hms.repository.AppointmentRepository;
import com.example.hms.repository.BillRepository;
import com.example.hms.repository.DoctorRepository;
import com.example.hms.repository.PatientRepository;
import com.example.hms.service.AppointmentService;
import com.example.hms.service.BillingService;
import com.example.hms.service.DoctorService;
import com.example.hms.service.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Optional demonstration records for presentations and manual exploration.
 *
 * <p>The bean exists only when {@code hospital.demo-data.enabled} is true
 * ({@code DEMO_DATA} in Docker Compose), and it writes only when the database has
 * no patients, doctors, appointments, or bills. Real records are therefore never
 * mixed with demo records, and restarting an already seeded application adds
 * nothing. Every record is created through the application services, so the demo
 * data satisfies the same validation, slot, and billing rules as staff input.
 */
@Component
@ConditionalOnProperty(prefix = "hospital.demo-data", name = "enabled", havingValue = "true")
@Order(20)
public class DemoDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final BillingService billingService;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final BillRepository bills;
    private final Clock clock;

    public DemoDataInitializer(PatientService patientService, DoctorService doctorService,
                               AppointmentService appointmentService, BillingService billingService,
                               PatientRepository patients, DoctorRepository doctors,
                               AppointmentRepository appointments, BillRepository bills, Clock clock) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.billingService = billingService;
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.bills = bills;
        this.clock = clock;
    }

    /** Records added by one seeding attempt; {@link #NONE} means an existing database was left alone. */
    public record DemoData(int doctors, int patients, int appointments, int bills, int payments) {
        public static final DemoData NONE = new DemoData(0, 0, 0, 0, 0);

        public boolean isEmpty() {
            return this.equals(NONE);
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        DemoData added = seed();
        if (added.isEmpty()) {
            log.info("Demo data was requested but this database already has records; nothing was added.");
        } else {
            log.info("Added demo data: {} doctors, {} patients, {} appointments, {} bills, {} payments.",
                    added.doctors(), added.patients(), added.appointments(), added.bills(), added.payments());
        }
    }

    /**
     * Adds the blueprint's demonstration records in one transaction, or returns
     * {@link DemoData#NONE} when the database is not empty.
     */
    @Transactional
    public DemoData seed() {
        if (patients.count() > 0 || doctors.count() > 0 || appointments.count() > 0 || bills.count() > 0) {
            return DemoData.NONE;
        }
        List<Doctor> practitioners = seedDoctors();
        List<Patient> registrations = seedPatients();
        List<Appointment> visits = seedAppointments(registrations, practitioners);
        return seedBilling(registrations, visits, practitioners.size());
    }

    private List<Doctor> seedDoctors() {
        // The dermatologist is unavailable so the availability filter and the blocked-booking rule are visible.
        return List.of(
                doctor("Karim", "Ahmed", "01800123401", "General Medicine", "MBBS, FCPS (Medicine)", "500.00", true),
                doctor("Nasrin", "Sultana", "01800123402", "Cardiology", "MBBS, MD (Cardiology)", "1200.00", true),
                doctor("Rafiqul", "Islam", "01800123403", "Orthopedics", "MBBS, MS (Orthopedics)", "900.00", true),
                doctor("Shirin", "Akter", "01800123404", "Pediatrics", "MBBS, DCH", "700.00", true),
                doctor("Tanvir", "Hossain", "01800123405", "Dermatology", "MBBS, DDV", "650.00", false));
    }

    private List<Patient> seedPatients() {
        // Fixed birth dates stay in the past; the last record shows the optional fields left blank.
        return List.of(
                patient("Amina", "Rahman", "01711000001", Gender.FEMALE, LocalDate.of(1996, 4, 12), "A+",
                        "House 14, Road 7, Dhanmondi, Dhaka", "Kamal Rahman 01711000011"),
                patient("Sabbir", "Chowdhury", "01711000002", Gender.MALE, LocalDate.of(1988, 11, 3), "B+",
                        "Sector 4, Uttara, Dhaka", "Rina Chowdhury 01711000012"),
                patient("Nusrat", "Jahan", "01711000003", Gender.FEMALE, LocalDate.of(2001, 7, 21), "O+",
                        "Mirpur 10, Dhaka", "Selim Jahan 01711000013"),
                patient("Imran", "Kabir", "01711000004", Gender.MALE, LocalDate.of(1975, 2, 17), "AB+",
                        "Agrabad, Chattogram", "Nadia Kabir 01711000014"),
                patient("Farzana", "Haque", "01711000005", Gender.FEMALE, LocalDate.of(1993, 9, 9), "A-",
                        "Zindabazar, Sylhet", "Rezaul Haque 01711000015"),
                patient("Mahmudul", "Hasan", "01711000006", Gender.MALE, LocalDate.of(1965, 5, 30), "O-",
                        "Kazir Dewri, Chattogram", "Sultana Hasan 01711000016"),
                patient("Rumana", "Begum", "01711000007", Gender.FEMALE, LocalDate.of(1982, 12, 25), "B-",
                        "Shaheb Bazar, Rajshahi", "Anwar Begum 01711000017"),
                patient("Arif", "Mahmud", "01711000008", Gender.MALE, LocalDate.of(2015, 3, 14), "O+",
                        "Bashundhara R/A, Dhaka", "Shahana Mahmud 01711000018"),
                patient("Shamima", "Nasrin", "01711000009", Gender.FEMALE, LocalDate.of(1999, 8, 8), "AB-",
                        "Boyra, Khulna", "Jamil Nasrin 01711000019"),
                patient("Jahangir", "Alam", "01711000010", Gender.NOT_SPECIFIED, null, null, null, null));
    }

    private List<Appointment> seedAppointments(List<Patient> people, List<Doctor> practitioners) {
        LocalDate today = LocalDate.now(clock);
        // One visit per demo patient: today's clinic, three billed past visits, and two future bookings.
        return List.of(
                appointment(people.get(0), practitioners.get(0), today, LocalTime.of(9, 0),
                        "Fever and sore throat for three days", null, AppointmentStatus.CONFIRMED),
                appointment(people.get(1), practitioners.get(1), today, LocalTime.of(9, 30),
                        "Chest discomfort while climbing stairs", "Bring previous ECG report.", AppointmentStatus.SCHEDULED),
                appointment(people.get(7), practitioners.get(3), today, LocalTime.of(10, 0),
                        "Routine growth and vaccination check", null, AppointmentStatus.CONFIRMED),
                appointment(people.get(3), practitioners.get(2), today, LocalTime.of(10, 30),
                        "Lower back pain after lifting a heavy load", null, AppointmentStatus.SCHEDULED),
                appointment(people.get(4), practitioners.get(0), today, LocalTime.of(11, 0),
                        "Follow-up consultation", "Patient rescheduled by phone.", AppointmentStatus.CANCELLED),
                appointment(people.get(2), practitioners.get(0), today.minusDays(1), LocalTime.of(10, 0),
                        "Persistent cough and fatigue", "Prescribed a seven-day course.", AppointmentStatus.COMPLETED),
                appointment(people.get(5), practitioners.get(1), today.minusDays(2), LocalTime.of(11, 30),
                        "High blood pressure review", "Advised a low-salt diet.", AppointmentStatus.COMPLETED),
                appointment(people.get(6), practitioners.get(2), today.minusDays(3), LocalTime.of(12, 0),
                        "Knee pain and swelling", "X-ray taken; physiotherapy advised.", AppointmentStatus.COMPLETED),
                appointment(people.get(8), practitioners.get(3), today.plusDays(1), LocalTime.of(9, 0),
                        "Child vaccination schedule advice", null, AppointmentStatus.SCHEDULED),
                appointment(people.get(9), practitioners.get(1), today.plusDays(2), LocalTime.of(16, 0),
                        "Cardiology consultation on referral", null, AppointmentStatus.SCHEDULED));
    }

    /** Three invoices for completed visits plus two standalone invoices cover every payment status and method. */
    private DemoData seedBilling(List<Patient> people, List<Appointment> visits, int doctorCount) {
        List<Bill> invoices = new ArrayList<>();
        int paymentCount = 0;

        Bill partiallyPaid = bill(people.get(2), visits.get(5), "500.00", "200.00", "150.50", "25.00", "75.50");
        invoices.add(partiallyPaid);
        pay(partiallyPaid.getId(), "300.00", PaymentMethod.CARD, "CARD-4411");
        paymentCount++;

        Bill settled = bill(people.get(5), visits.get(6), "1200.00", "300.00", "0.00", "0.00", "0.00");
        invoices.add(settled);
        pay(settled.getId(), "1000.00", PaymentMethod.MOBILE_BANKING, "BKASH-90231");
        pay(settled.getId(), "500.00", PaymentMethod.CASH, "COUNTER-118");
        paymentCount += 2;

        invoices.add(bill(people.get(6), visits.get(7), "900.00", "150.00", "220.75", "0.00", "70.75"));

        Bill advancePaid = bill(people.get(3), null, "0.00", "400.00", "250.50", "50.00", "0.00");
        invoices.add(advancePaid);
        pay(advancePaid.getId(), "700.50", PaymentMethod.BANK_TRANSFER, "TRX-550120");
        paymentCount++;

        invoices.add(bill(people.get(9), null, "0.00", "250.00", "125.25", "0.00", "0.00"));

        return new DemoData(doctorCount, people.size(), visits.size(), invoices.size(), paymentCount);
    }

    private Doctor doctor(String firstName, String lastName, String phone, String specialization,
                          String qualification, String consultationFee, boolean available) {
        DoctorForm form = new DoctorForm();
        form.setFirstName(firstName);
        form.setLastName(lastName);
        form.setPhone(phone);
        form.setEmail(email(firstName, lastName));
        form.setSpecialization(specialization);
        form.setQualification(qualification);
        form.setConsultationFee(new BigDecimal(consultationFee));
        form.setAvailable(available);
        return doctorService.createDoctor(form);
    }

    private Patient patient(String firstName, String lastName, String phone, Gender gender, LocalDate dateOfBirth,
                            String bloodGroup, String address, String emergencyContact) {
        PatientForm form = new PatientForm();
        form.setFirstName(firstName);
        form.setLastName(lastName);
        form.setPhone(phone);
        form.setEmail(dateOfBirth == null ? null : email(firstName, lastName));
        form.setGender(gender);
        form.setDateOfBirth(dateOfBirth);
        form.setBloodGroup(bloodGroup);
        form.setAddress(address);
        form.setEmergencyContact(emergencyContact);
        return patientService.createPatient(form);
    }

    private Appointment appointment(Patient patient, Doctor doctor, LocalDate date, LocalTime time,
                                    String reason, String notes, AppointmentStatus status) {
        AppointmentForm form = new AppointmentForm();
        form.setPatientId(patient.getId());
        form.setDoctorId(doctor.getId());
        form.setAppointmentDate(date);
        form.setAppointmentTime(time);
        form.setReason(reason);
        form.setNotes(notes);
        Appointment booked = appointmentService.createAppointment(form);
        Long id = booked.getId();
        // Statuses follow the real transitions; each one submits the version it just read.
        switch (status) {
            case SCHEDULED -> { }
            case CONFIRMED -> appointmentService.confirmAppointment(id, version(id));
            case COMPLETED -> {
                appointmentService.confirmAppointment(id, version(id));
                appointmentService.completeAppointment(id, version(id));
            }
            case CANCELLED -> appointmentService.cancelAppointment(id, version(id));
        }
        return booked;
    }

    private Long version(Long appointmentId) {
        return appointmentService.getAppointment(appointmentId).getVersion();
    }

    private Bill bill(Patient patient, Appointment appointment, String consultationFee, String serviceCharge,
                      String medicineCharge, String otherCharge, String discount) {
        CreateBillForm form = new CreateBillForm();
        form.setPatientId(patient.getId());
        if (appointment != null) {
            form.setAppointmentId(appointment.getId());
        }
        form.setConsultationFee(new BigDecimal(consultationFee));
        form.setServiceCharge(new BigDecimal(serviceCharge));
        form.setMedicineCharge(new BigDecimal(medicineCharge));
        form.setOtherCharge(new BigDecimal(otherCharge));
        form.setDiscount(new BigDecimal(discount));
        return billingService.createBill(form);
    }

    private void pay(Long billId, String amount, PaymentMethod method, String reference) {
        PaymentForm form = new PaymentForm();
        form.setAmount(new BigDecimal(amount));
        form.setPaymentMethod(method);
        form.setTransactionReference(reference);
        form.setVersion(billingService.getBill(billId).getVersion());
        billingService.recordPayment(billId, form);
    }

    private String email(String firstName, String lastName) {
        return (firstName + "." + lastName + "@example.com").toLowerCase(Locale.ROOT);
    }
}
