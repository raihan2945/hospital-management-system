# Hospital Management System

A Java 21 varsity assignment project following the [project blueprint](docs/hospital-management-system--blueprint.md).

Phases 1 through 11 provide a running Spring Boot application, persistent
PostgreSQL storage, the common OOP foundation, and complete patient and doctor
directories, appointment management, billing, invoices, payment history, and a
dashboard with current totals and recent activity. Phase 9 adds shared desktop/mobile
navigation, accessible UI improvements, and confirmation dialogs. Phase 10 adds
consistent validation, hospital-date checks, and safe error handling. Phase 11
adds optional demo data for presentations.

## Stack and structure

- Java 21, Maven, Spring Boot 3.5.16
- Spring MVC, Thymeleaf, Bootstrap 5.3.8 (served locally from a WebJar)
- Spring Data JPA, Jakarta Bean Validation, PostgreSQL 17
- Spring Boot Actuator for application and database health
- Docker multi-stage build and Docker Compose

```text
src/main/java/com/example/hms/
  HospitalManagementApplication.java
  controller/             Dashboard, patient, doctor, appointment, and billing controllers
  config/                 Registration code initialization, hospital clock, and demo data
  domain/                 BaseEntity, Person, Patient, Doctor, Appointment, Bill, Payment
  domain/enums/           Gender, AppointmentStatus, PaymentStatus, PaymentMethod
  dto/                    Management forms and immutable dashboard summaries
  repository/             Repositories, directory search, and scheduling locks
  service/                Directory, appointment, billing, and dashboard services
  service/payment/        PaymentProcessor and four payment recording implementations
  exception/              Application errors and handling
  validation/             Cross-field bill discount validation
  util/                   Shared helpers
src/main/resources/
  application.yml
  templates/dashboard/    Overview cards, quick actions, and recent activity
  templates/layout/fragments.html
  templates/layout/forms.html
  templates/patients/      List, form, and details pages
  templates/doctors/       List, form, and details pages
  templates/appointments/  List, form, details, status, and confirmation pages
  templates/billing/       List, create bill, invoice, and payment pages
  templates/directory/    Delete confirmation
  templates/error/        Friendly missing-record and invalid-request pages
  static/css/app.css
  static/js/billing.js      Decimal total preview and appointment selection
  static/js/app.js          Confirmation dialogs, form feedback, submission guard
  static/vendor/           Locally served Bootstrap Icons and license
src/test/                 Domain, persistence, startup, page, and health tests
src/test/ui/              JavaScript interaction tests with Bootstrap and jsdom
scripts/test-ui.ps1        Isolated Docker runner for JavaScript tests
```

The base packages are documented with `package-info.java`. Features follow controller → service → repository
→ PostgreSQL, with entities representing domain state and DTOs validating forms.
The dashboard controller obtains a read-only summary from its service. Shared
Thymeleaf fragments provide the page head, navigation, and footer.

## Phase 3: common domain and OOP foundation

```text
BaseEntity (abstract, @MappedSuperclass)
  └── Person (abstract, @MappedSuperclass)
        ├── Patient (@Entity → patients)
        └── Doctor  (@Entity → doctors)
```

`BaseEntity` owns a generated database ID and UTC `Instant` creation/update
timestamps. JPA lifecycle callbacks maintain those timestamps. Callers can read
them but cannot assign an ID or change audit timestamps through setters.

`Person` owns private first name, last name, phone, and optional email fields.
Both subclasses inherit `getFullName()`, `rename()`, and `updateContact()`.
These methods normalize surrounding whitespace and reject missing required
values or excessive lengths before changing state. Jakarta Bean Validation also
checks the inherited fields, including email format, when entities are persisted.
Form DTO validation also runs in the management controllers and service entry points.

`Patient` adds `Gender` and an optional date of birth, rejecting future dates.
Gender is stored by enum name (`MALE`, `FEMALE`, `OTHER`, `NOT_SPECIFIED`).
`Doctor` adds specialization, a nonnegative consultation fee with at most two
decimal places, and explicit availability operations. Both entities provide
protected no-argument constructors for JPA and public constructors for valid
application initialization.

This demonstrates **encapsulation** through private fields and controlled update
methods, and **inheritance** through shared person and persistence behavior.
`@MappedSuperclass` puts inherited columns in `patients` and `doctors`; it does
not create separate `person` or `base_entity` tables. IDs belong to each concrete
table, so a patient and doctor may have the same numeric ID.

For example:

```java
Person patient = new Patient("Amina", "Rahman", "01700123456", null,
        Gender.FEMALE, LocalDate.of(2000, 1, 15));
Person doctor = new Doctor("Karim", "Ahmed", "01800123456", null,
        "General Medicine", new BigDecimal("500.00"));

patient.rename("Amina", "Khan");
System.out.println(patient.getFullName()); // Amina Khan
System.out.println(doctor.getFullName());  // Karim Ahmed
```

Phases 4 and 5 extend these entities with registration fields and the management
flows described below.

## Phases 4 and 5: patient and doctor management

Open [Patients](http://localhost:8080/patients) or
[Doctors](http://localhost:8080/doctors) from the navigation or home page.

| Feature | Patients | Doctors |
| --- | --- | --- |
| Registration and editing | Name, phone, email, gender, birth date, address, blood group, emergency contact | Name, phone, email, specialization, qualification, consultation fee, availability |
| Generated code | `PAT-000001` | `DOC-000001` |
| Search | Full name, code, phone, email | Full name, code, phone, email |
| Filters | Gender, blood group | Specialization, availability |
| Directory | Ten records per page, newest first | Ten records per page, newest first |
| Details and deletion | Full profile and confirmation before deletion | Full profile and confirmation before deletion |

Both modules use these routes, with `patients` or `doctors` as the directory:

| Method | Route | Behavior |
| --- | --- | --- |
| GET | `/patients`, `/doctors` | Searchable, paginated directory |
| GET | `/{directory}/new` | Registration form |
| POST | `/{directory}` | Validate and create |
| GET | `/{directory}/{id}` | Details |
| GET / POST | `/{directory}/{id}/edit` | Load / validate and save edits |
| GET | `/{directory}/{id}/delete` | Show confirmation only |
| POST | `/{directory}/{id}/delete` | Permanently delete the selected record |

Controllers bind editable DTO fields only, show field errors, and redirect after
successful changes. Services own transactions, mapping to domain update methods,
code assignment, queries, and deletion. Repositories use parameterized criteria
queries. Search is case-insensitive and treats `%`, `_`, and `!` literally.
Page links retain the search and filters. Shared phone numbers and email addresses
are allowed; registration codes uniquely identify records.

Codes are generated from database IDs inside the creation transaction and remain
stable after edits. Database unique constraints protect them. Code columns permit
null during the initial insert, before the generated ID is known. The startup
initializer assigns codes to any existing phase 3 records that lack them. It
preserves their IDs and creation times. Rebuild normally to upgrade; do not reset
the database volume. No demo patient or doctor records are added automatically
unless demo data is explicitly requested; see [Phase 11](#phase-11-demo-data).

Specialization suggestions allow custom values; the list filter uses the actual
stored specializations. Consultation fees accept zero through `9999999999.99`,
with at most two decimal places. Invalid dates, malformed fees, missing required
fields, and invalid email addresses are shown on the form while preserving input.
Missing records return a friendly HTTP 404, invalid URL/filter types return 400,
and database constraint conflicts return 409.

Deletion removes an unreferenced record permanently after confirmation. Patients
and doctors with any appointment history, including cancelled appointments, cannot
be deleted. Foreign keys also protect those relationships. Mark a doctor
unavailable when they stop taking bookings. Patients with billing history are also
protected from deletion, including those with no appointments.

### Manual verification for each module

1. Register a patient with blood group and emergency contact. Verify the assigned
   code and details. Edit the contact information and verify the code stays the same.
2. Search by full name, code, or phone, then combine gender and blood-group filters.
   With more than ten matching records, follow Next and Previous and check that
   the filters remain selected.
3. Register a doctor with specialization, qualification, and consultation fee.
   Edit the doctor, uncheck availability, and verify the directory and details.
4. Combine specialization and availability filters; search by doctor code.
5. Try a future birth date, invalid email, and negative or overly precise fee.
   Expect field errors and no saved changes. Server validation also works when
   browser validation is bypassed.
6. Open Delete for a disposable record and choose Cancel; the record should remain.
   Then confirm deletion and verify its detail URL returns 404.
7. Restart the application with `docker compose restart app`; saved records and
   their codes should remain available.

## Phase 6: appointment management

Open [Appointments](http://localhost:8080/appointments), or use the appointment
links on a patient or doctor profile. A booking connects an existing patient and
doctor with a date, time, optional reason/symptoms, and notes. Appointment codes
(`APT-000001`) are generated from database IDs and remain stable after changes.

The list has date, patient, doctor, and status filters, with ten records per page,
newest appointment date/time first. Pagination preserves the filters. Patient
and doctor profile links open the corresponding filtered appointment history.
Related people are fetched with each list/detail query, so rendering does not
depend on an open database session in the view.

| Method | Route | Behavior |
| --- | --- | --- |
| GET | `/appointments` | Filtered list |
| GET | `/appointments/new` | Booking form; optional `patientId`/`doctorId` preselection |
| POST | `/appointments` | Validate and schedule |
| GET | `/appointments/{id}` | Details and status actions |
| GET / POST | `/appointments/{id}/edit` | Load / validate and save changes |
| GET / POST | `/appointments/{id}/confirm` | Review / confirm a scheduled booking |
| GET / POST | `/appointments/{id}/cancel` | Review / cancel and free its slot |
| GET / POST | `/appointments/{id}/complete` | Review / mark the visit complete |

Status transitions are:

```text
SCHEDULED → CONFIRMED → COMPLETED
    │          │
    ├──────────┴────→ CANCELLED
    └───────────────→ COMPLETED
```

- New bookings start `SCHEDULED`. Forms cannot assign codes or arbitrary statuses.
- Scheduled and confirmed bookings can be edited. Changing their patient, doctor,
  date, or time returns a confirmed booking to `SCHEDULED`; notes-only edits retain
  confirmation.
- Completed and cancelled bookings are read-only and retained as history. There
  is no appointment deletion or reopening endpoint.
- Only cancelled appointments release their slot. Completed visits continue to
  occupy their original slot, matching the blueprint's non-cancelled conflict rule.
- An unavailable doctor cannot receive a new or moved booking. Existing bookings
  can still have reason/notes updated and can be confirmed, cancelled, or completed.
- Dates and times are required. Slots use whole minutes in the hospital's local
  time; seconds and fractional seconds are rejected. Past dates are allowed for
  recording historical visits; the blueprint does not require future-only booking.
- A slot conflict or missing/unavailable selection returns field errors with the
  entered values. Invalid transitions and stale edits return HTTP 409. Missing
  appointments return 404, and malformed filter values return 400.

### Concurrent booking and stale edits

`AppointmentService` checks each doctor's slot while holding a pessimistic write
lock on that doctor's database row until the transaction commits. Create, edit,
confirm, cancel, and complete operations follow that locking convention. Moves
between doctors lock both rows in ID order. Doctor availability updates also use
the doctor lock. This prevents two application requests from both reserving the
same slot. `isDoctorAvailable()` is an advisory read; write operations always
check again under the lock.

Appointments also carry a JPA version. Edit and status forms submit the version
they loaded; a changed or missing version cannot overwrite newer work. The
database supplies foreign keys, a unique appointment-code constraint, and slot
lookup indexes. Conflict prevention uses service transactions rather than a
database partial unique index, so appointment writes should go through the service.

### Manual appointment verification

1. Register a patient and an available doctor, then schedule a visit. Check its
   generated code, date/time, reason, and linked profiles.
2. Try another booking with the same doctor/date/time. Expect a time-field error
   and no second booking. Another doctor or another time should work.
3. Edit the original booking without moving it; it should not conflict with itself.
   Confirm it, then move it and check that it returns to Scheduled.
4. Open an edit form in two tabs. Save one, then submit the other. Expect HTTP 409
   and an instruction to reload.
5. Cancel a booking and reuse its slot. Complete another booking; its edit/cancel
   actions should be unavailable and direct requests should be rejected.
6. Combine all four list filters, follow page links, and open appointment history
   from both related profiles.
7. Try deleting the referenced patient or doctor. Expect a clear conflict message
   and retained history. Restart the app and verify that bookings remain available.

## Phase 7: billing and payments

Open [Billing](http://localhost:8080/billing), or use the billing links on a
patient or appointment profile. Create a standalone patient bill or link one to
an unbilled, non-cancelled appointment belonging to that patient. Selecting an
appointment suggests its doctor's consultation fee; staff can adjust the charge
before issuing the invoice. Each appointment can have at most one bill, protected
by both a database unique constraint and an appointment row lock.

| Method | Route | Behavior |
| --- | --- | --- |
| GET | `/billing` | Paginated list filtered by patient, payment status, or linked appointment |
| GET | `/billing/new` | Create form; optional `patientId` or `appointmentId` preselection |
| POST | `/billing` | Validate charges, calculate totals, and issue invoice |
| GET | `/billing/{id}` | Invoice, payment history, and Print invoice button |
| GET / POST | `/billing/{id}/payment` | Payment form / record received payment |

Invoices receive stable `INV-000001` numbers generated from database IDs. They
retain the patient name/code and charge amounts captured when issued. They cannot
be edited or deleted through the app. Billed appointments cannot be edited or
cancelled, but can still be confirmed or completed. The invoice's patient link
opens the current directory profile.

`BillingCalculator` provides the abstraction used by `BillingService`.
`StandardBillingCalculator` calculates consultation + service + medicine + other
charges, minus discount. `BigDecimal` arithmetic preserves cents. Subtotal and
due are derived from stored charges, total, and paid amount to avoid inconsistent
duplicate values. Individual charges allow zero through `9999999999.99` with at
most two decimal places. Discounts cannot exceed subtotal. A zero-total invoice
is immediately Paid with no payment entry. All amounts use one hospital accounting
currency; currency conversion and tax calculation are outside this phase.

The `PaymentProcessor` interface is selected polymorphically through injected
cash, card, mobile banking, and bank transfer implementations. These record money
already received outside the app; they do not charge cards or contact gateways.
Each payment retains its amount, method, optional transaction reference, and UTC
timestamp. Payment history is append-only through the application.

Positive payments may cover part or all of the balance. Status changes from
Unpaid to Partially paid to Paid automatically. Overpayments, negative amounts,
excess precision, and malformed values are rejected. Payment writes lock the bill
row and compare the submitted version, so simultaneous requests and resubmitted
forms cannot record the same balance twice. A stale form returns HTTP 409 and
asks staff to reload and check payment history. Reloading and intentionally
submitting a new form records a new payment; external reference numbers are not
used as unique payment identifiers.

The create form previews totals in integer cents, while server validation and
calculation remain authoritative and work without JavaScript. Print invoice uses
the browser print dialog; print CSS removes navigation and actions and retains
charges and payment history. No PDF generation dependency is needed.

### Manual billing verification

1. Open Create bill from an appointment. Check the patient and suggested fee.
   Enter consultation `500`, service `200`, medicine `150.50`, other `25`, and
   discount `75.50`. Verify subtotal `875.50`, total/due `800.00`, and Unpaid.
2. Try billing the same appointment again, linking another patient's appointment,
   entering a negative charge, or discounting more than the subtotal. Expect no
   additional invoice and a clear error. A standalone bill should also work.
3. Record `300` using any method and a receipt reference. Verify Partially paid,
   paid `300.00`, due `500.00`, and the payment history row. Try paying `500.01`;
   expect an error. Record `500` and verify Paid with no further payment action.
4. Open a payment form in two tabs before paying. Submit one, then the other;
   expect HTTP 409 with no duplicate payment. Confirm all four methods are offered.
5. Filter invoices by patient and status, follow pagination, and check patient
   billing history. On the invoice, choose Print invoice and inspect print preview.
6. Try deleting a patient with a standalone bill. Expect a conflict and preserved
   history. A billed appointment should allow completion but block edit/cancel.
7. Restart the app and confirm invoice numbers, balances, and payment history
   remain. Existing database contents are retained during the Phase 7 upgrade.

## Phase 8: dashboard

Open [Dashboard](http://localhost:8080/dashboard) or the root URL. Both now serve
the same live overview, replacing the starter home page. Bootstrap cards display
patient and doctor counts, today's appointments, pending appointments, total
revenue, and outstanding due. Additional links show total appointments, completed
appointments, and total bills. Quick actions open registration, booking, and bill
creation; card links open the relevant directories and filtered lists.

| Metric | Definition |
| --- | --- |
| Patients / doctors | All registered records, including unavailable doctors |
| Today's appointments | All appointments dated today in the hospital timezone, including completed and cancelled visits |
| Pending appointments | Scheduled plus confirmed appointments across all dates |
| Completed appointments | Appointments with Completed status across all dates |
| Total bills | All issued invoices, including zero-total invoices |
| Total revenue | Sum of recorded payment amounts, including partial payments; unpaid invoice amounts are not revenue |
| Outstanding due | Sum of invoice total minus paid amount across all bills |

The hospital timezone defaults to `Asia/Dhaka` and is configurable through
`HOSPITAL_TIME_ZONE` in Compose or the local Java environment. This avoids using
the container's UTC date for today's visits. The displayed date and refresh time
use the same clock instant. Stored audit and payment timestamps remain UTC.

`DashboardService` uses database count/sum queries with exact decimal amounts and
returns `0.00` when there are no bills or payments. A read-only PostgreSQL
repeatable-read transaction keeps each summary internally consistent. Recent
patients and appointments are limited to five each, ordered by creation timestamp
then ID descending. Recent appointments mean newly created bookings, regardless
of the scheduled visit date. Related people are fetched in the query and mapped
to immutable summary records before rendering, so no open view transaction is
needed. Opening the dashboard does not add or change any records.

Refresh dashboard reloads the latest values; there is no automatic polling or
external chart dependency. Empty states link to the relevant registration and
booking forms. All-time counts and financial amounts are labelled explicitly.

### Manual dashboard verification

1. Open `/` and `/dashboard`; both should display identical metrics and recent
   lists. With an empty database, verify zero values and registration prompts.
2. Register a patient and doctor, refresh, and check their counts and the recent
   patient link. Schedule an appointment for the displayed local date; today's
   and pending counts should increase. Follow the today card to verify its filter.
3. Confirm then complete the appointment, refreshing after each change. Pending
   includes both Scheduled and Confirmed, then decreases on completion. Cancelled
   appointments remain in total/today counts but are excluded from pending.
4. Create an unpaid `800.00` bill; outstanding due increases by `800.00`, while
   revenue remains unchanged. Record `300.00`; revenue increases by `300.00` and
   outstanding due falls by `300.00`. Record the remaining `500.00` and check again.
5. Create more than five patients/bookings; recent lists should show only the
   latest five, with links to their details. Check the page at narrow widths.
6. If the hospital uses another timezone, set `HOSPITAL_TIME_ZONE` to its IANA
   timezone and recreate the Compose app. Check the displayed date/time and today's
   appointment filter. Restart normally and verify the persisted data still drives
   the same totals.

## Phase 9: UI/UX improvements

Every page now shares a top navigation bar and a desktop sidebar. Below 992px the
sidebar becomes a Bootstrap offcanvas menu, with labelled open/close controls and
keyboard dismissal. The current module is highlighted with `aria-current`.
Quick registration and booking links are available from the shared navigation.
Without JavaScript, mobile navigation remains visible as ordinary links.

The interface includes locally served [Bootstrap Icons v1.13.1](https://github.com/twbs/icons/releases/tag/v1.13.1).
Only eleven required symbols are included, with their original MIT license in
`static/vendor/bootstrap-icons/`. No external icon fonts or CDN requests are used.
Consistent empty states provide a next action in each directory. Tables have
labelled, keyboard-focusable horizontal scroll regions and mobile scroll hints.
Form fields have larger touch targets, clearer labels, visible focus outlines,
and decimal-keyboard hints for money. Validation errors retain input and move
focus to the error summary; success alerts can be dismissed and do not expire.

Appointment statuses retain blue/info/green/red badges. Billing now consistently
uses green for Paid, orange for Partially paid, and red for Unpaid on both lists
and invoices. Status text remains visible alongside the color. Billed appointments
no longer show an Edit link in the appointment list; their existing server-side
edit protection remains in force.

Delete links and appointment confirm/complete/cancel links open a confirmation
modal when JavaScript and Bootstrap are available. The dialog fetches the existing
confirmation page using GET and uses its actual POST form, including the current
appointment version. Opening or closing the dialog never changes a record.
Cancel receives initial focus; Escape, Cancel, and Close dismiss the dialog and
return focus to the triggering link. The original confirmation URLs remain usable
without JavaScript, with modified clicks, or if fetching the dialog fails.

Valid POST forms guard rapid repeated submissions and expose a busy state. Browser
back/forward navigation resets that client-side guard; billing version checks still
provide the server-side protection. Reduced-motion preferences are respected.
Invoice printing hides the shared navigation and restores full-page invoice width.

### Manual UI verification

1. Open the dashboard, directories, and forms at desktop and narrow mobile widths.
   Check sidebar highlighting, the mobile menu, table scrolling, wrapped actions,
   and form readability. Use Tab to navigate and confirm focus stays visible.
2. Open Delete for a disposable patient or doctor. Check the name/code, choose
   Cancel or press Escape, and verify that focus returns to the link and the record
   remains. Open the direct confirmation URL to verify the page fallback.
3. Open appointment confirmation/cancellation/completion. Verify its details, close
   without changing status, then confirm a suitable test appointment and check the
   success alert. Billed appointments should not offer Edit in the list.
4. Submit an invalid form. Check preserved input, the focused error summary, and
   field errors. Complete a valid operation and dismiss its success message.
5. Check Unpaid, Partially paid, and Paid badges on invoices and billing lists.
   Print an invoice and verify navigation is absent and the page uses full width.
6. Disable JavaScript and verify mobile navigation, form submission, and direct
   confirmation pages still work. Re-enable it afterwards.

### JavaScript interaction tests

The six interaction tests run Bootstrap's actual JavaScript against jsdom to
check modal fetching, version preservation, escaped names, focus, Cancel/Escape,
rapid clicks, native link fallback, submission guards, and error accessibility.
They do not replace visual desktop/mobile checks. Browser automation was unavailable
in this workspace during Phase 9, so those visual checks remain manual.

With Docker running, execute from PowerShell:

```powershell
.\scripts\test-ui.ps1
```

The script uses a temporary Node container and a read-only workspace mount;
dependencies are installed only in that container. Alternatively, with Node 22:

```shell
cd src/test/ui
npm ci
npm test
```

These JavaScript tests run separately from Maven. The Java suite also verifies
shared navigation, local asset references, form feedback, confirmation-page
fallbacks, and visibility of actions on billed appointments.

## Phase 10: validation and exception handling

All create/update service forms now require a non-null DTO and run Bean Validation
at the service boundary as well as in MVC. Billing and payment fields have explicit
messages for missing, negative, oversized, or overly precise values. Malformed
amounts, payment methods, appointment selections, and versions have field-specific
binding messages. Invalid forms preserve the other entered values and show the
existing field errors and focused summary.

`@ValidBillDiscount` validates the combined charges before entering billing logic
and attaches an excessive-discount error to the Discount field. The domain still
checks that rule independently. Payment references are trimmed consistently with
other optional text. `InvalidPaymentException` identifies invalid settlement
amounts; `DuplicateBillException` identifies an already-billed appointment.

Duplicate protection remains based on record identity and business relationships:
registration/invoice codes have database unique constraints, doctor slots are
checked under row locks, each appointment has at most one bill, and bill versions
protect repeated/concurrent payment submissions. Shared names, phones, and emails
remain allowed; those fields are not reliable unique patient identifiers.

A patient can no longer be booked twice into the same date and time, which two
different doctors previously allowed. Booking and rescheduling lock the selected
patient row before the doctor rows, so two concurrent requests for one patient are
serialized in the same way as competing bookings for one doctor, and the fixed
patient-then-doctor lock order keeps those transactions deadlock-free.
`PatientScheduleConflictException` reports the clash on the Patient field, while a
busy doctor still reports on Appointment time. Cancelled bookings free both slots,
and an appointment never conflicts with itself while being edited. Two different
patients may still see two doctors at the same time.

Birth-date validation now uses `HOSPITAL_TIME_ZONE` consistently in the form's max
date, MVC and service Bean Validation, domain updates through the patient service,
and JPA persistence validation. This accepts a newborn born today in the hospital
timezone even when the container's UTC calendar still shows yesterday. Direct
domain callers can supply their current date explicitly; existing overloads use
the JVM's local date. Tomorrow is still rejected.

Negative page numbers and offsets beyond JPA's supported integer range produce
HTTP 400 rather than a database error. The shared exception handler and servlet
error controller provide these responses:

| Status | Behavior |
| --- | --- |
| 400 | Malformed parameters, invalid page numbers, binding/service validation, or uncaught business validation |
| 404 | Missing records, unknown pages, missing static resources, or a direct `/error` visit |
| 405 | Unsupported HTTP method, retaining the `Allow` response header |
| 406 / 415 | Unsupported response/request formats |
| 409 | Duplicate database records, referenced records, stale versions, lock conflicts, or query timeouts |
| 503 | Unavailable database connection/transaction |
| 500 | Unexpected failure, with a support reference linking to the server log |

Entity-level Bean Validation that fails while the transaction is being rolled back
arrives wrapped in `TransactionSystemException`; it is unwrapped to its root cause
so a rejected value produces 400 rather than the 500 page, while any other rollback
cause keeps the logged 500 with its support reference.


Error pages never render raw exception messages, SQL, rejected values, or stack
traces from unexpected/framework failures. Deliberately written business messages
remain visible. Servlet fallback errors use the same safe page, and Spring's error
detail settings prevent query parameters such as `trace=true` from exposing a
stack trace. Payment-related recovery text asks staff to inspect payment history
before resubmitting because a connection failure can leave the result uncertain.

### Manual validation verification

1. Submit a patient/doctor form with required fields missing, a malformed email, or
   a future birth date. Expect field errors, retained input, and no saved record.
2. Enter malformed/negative billing charges or a discount above the subtotal.
   Expect a message identifying the field. Try an invalid payment method, amount,
   or missing version and check that the payment history does not change.
3. Try duplicate doctor slots, a second bill for one appointment, and a repeated
   payment submission. Verify the existing record remains intact and the conflict
   is explained. Two patients may still share contact details.
4. Book one patient at a date and time, then try the same patient with a different
   doctor at that time. Expect an error on the Patient field and no second booking.
   Cancel the first booking and confirm the slot can be used again.
5. Open `/patients?page=-1`, `/billing?page=2147483647`, and an unknown URL. Expect
   friendly 400/404 pages with working navigation. Add `?trace=true` to the unknown
   URL and verify no technical details appear.
6. On a disposable local setup, stop PostgreSQL and open a data page. Expect a 503
   page; restart PostgreSQL before continuing. Check payment history before retrying
   a write interrupted by an outage.

The automated suite adds error-response tests for unavailable databases, locks,
validation exceptions, integrity conflicts, rollback causes, unknown routes,
unsupported methods, and unexpected failures. Appointment tests cover the patient
slot rule for new bookings, reschedules, self-edits, and cancelled bookings. It also tests preserved billing input, excessive discounts,
null service forms, supported page bounds, and shared contacts. Isolated clock tests
verify newborn registration through MVC, service/domain logic, and JPA across the
hospital/UTC midnight boundary.

## Phase 11: demo data

Demo records make the final presentation easy to run without registering
everything by hand. They are opt-in: set `DEMO_DATA=true` in `.env` (or in the
environment of a local Java process), then start the application normally.

```shell
docker compose up -d --wait
```

`DemoDataInitializer` exists only while that setting is true, and it writes only
when the database has no patients, doctors, appointments, or bills. Demo records
are therefore never mixed into a database that already holds real work, and
restarting an already seeded application adds nothing. The startup log reports
which of the two happened:

```text
Added demo data: 5 doctors, 10 patients, 10 appointments, 5 bills, 4 payments.
Demo data was requested but this database already has records; nothing was added.
```

| Records | Contents |
| --- | --- |
| 5 doctors | General Medicine, Cardiology, Orthopedics, Pediatrics, and one unavailable Dermatology doctor; fees `500.00` to `1200.00` |
| 10 patients | Fixed past birth dates, addresses, blood groups, and emergency contacts; the last record leaves the optional fields blank |
| 10 appointments | Five today (two Confirmed, two Scheduled, one Cancelled), three Completed past visits, and two future bookings |
| 5 bills | Three invoices for the completed visits and two standalone patient bills, covering Paid, Partially paid, and Unpaid |
| 4 payments | One cash, card, mobile banking, and bank transfer settlement with receipt references |

Appointment dates are relative to the hospital timezone's current date, so
today's appointment card and date filter are always populated. After seeding, the
dashboard shows ten patients, five doctors, five appointments today, six pending,
total revenue `2500.50`, and outstanding due `2075.25`.

All records are created through the same services the web forms use, inside one
transaction, so demo data satisfies the same Bean Validation, doctor and patient
slot rules, billing arithmetic, and ID-derived `PAT-`/`DOC-`/`APT-`/`INV-` codes
as staff input. A partial failure rolls back completely. `data.sql` and a Flyway
migration were both considered; a startup runner was chosen because inserted SQL
rows would bypass those rules and could not derive codes from generated IDs.

To present on a clean database, remove the demo records with
`docker compose down -v` (this **deletes all local data**) and start again with
`DEMO_DATA=true`. Set `DEMO_DATA=false` and recreate the app to keep the existing
records without seeding another database later.

### Manual demo data verification

1. With an empty database and `DEMO_DATA=true`, start the application and check
   the startup log line and the populated dashboard cards and recent lists.
2. Restart the app. The log should report that nothing was added, and the counts
   should be unchanged.
3. Filter doctors by availability; only Tanvir Hossain should be unavailable, and
   booking him should be rejected. Filter appointments by today's date and by each
   status. Check the Partially paid, Paid, and Unpaid billing filters.
4. Open invoice `INV-000001`: total `800.00`, paid `300.00`, due `500.00`, with one
   card payment in history. `INV-000002` should be fully paid by two payments.
5. Register a new patient and book a visit alongside the demo records; both should
   behave normally. Then set `DEMO_DATA=false`, recreate the app, and verify that
   the existing records and codes remain untouched.

## Run with Docker (recommended)

Install Docker with Compose v2. On Windows, start Docker Desktop with Linux
containers. A local Java, Maven, or PostgreSQL installation is not needed.

1. Optionally copy the example configuration to customize local settings:

   ```powershell
   Copy-Item .env.example .env
   ```

   On macOS/Linux use `cp .env.example .env`. The defaults also work without `.env`.
   The sample credentials are for local development.

2. Build and start both services:

   ```shell
   docker compose up --build -d --wait --wait-timeout 180
   ```

   The first build downloads the JDK, Maven dependencies, and container images.
   Maven tests run during the image build. The application waits for PostgreSQL
   readiness; Compose then waits for the application health check.

3. Open [the dashboard](http://localhost:8080). `/dashboard` serves the same
   live overview. Open [system health](http://localhost:8080/actuator/health) and
   confirm both the overall status and `components.db.status` are `UP`.

Useful commands:

```shell
docker compose ps
docker compose logs --tail=100 app postgres
docker compose logs -f app
docker compose down
```

`docker compose down` preserves database contents in the named
`hospital_postgres_data` volume. Start again with `docker compose up -d --wait`.
`docker compose down -v` **deletes the project's database volume and all its data**;
use it only when deliberately resetting local data.

Both published ports bind to localhost. Change `SERVER_PORT` or `POSTGRES_PORT`
in `.env` if 8080 or 5432 is already in use, then run Compose again. For example,
`SERVER_PORT=8081` changes the browser address to `http://localhost:8081`; the app
still listens on port 8080 inside its container.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Host HTTP port in Compose; listening port for local Java |
| `HOSPITAL_TIME_ZONE` | `Asia/Dhaka` | IANA timezone for dashboard dates, today's appointments, and birth-date validation |
| `DEMO_DATA` | `false` | Add demo records at startup, but only to a database with no patients, doctors, appointments, or bills |
| `POSTGRES_PORT` | `5432` | Host PostgreSQL port in Compose |
| `POSTGRES_DB` | `hospital_db` | Database initialized by Compose |
| `POSTGRES_USER` | `hospital` | Database user initialized by Compose |
| `POSTGRES_PASSWORD` | `hospital` | Database password initialized by Compose |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/hospital_db` | JDBC URL for local Java |
| `DATABASE_USERNAME` | `hospital` | Database username for local Java |
| `DATABASE_PASSWORD` | `hospital` | Database password for local Java |
| `JPA_DDL_AUTO` | `update` | Hibernate schema management mode |
| `THYMELEAF_CACHE` | `true` | Template caching for local Java |

Compose derives the application's `DATABASE_*` values from `POSTGRES_*`, using
the internal host `postgres` and port `5432`. A local Java process uses
`localhost` and the published PostgreSQL port instead. Spring Boot does **not**
automatically load `.env`; export variables in the shell for local Java.

PostgreSQL initialization variables apply only to an empty data volume. Changing
credentials in `.env` does not modify an existing database user or database.

Hibernate `update` is the blueprint's early development setting. Startup creates
or extends the `patients`, `doctors`, `appointments`, `bills`, and `payments` tables. Schema migrations can be
introduced as the domain modules grow.
Only the Actuator health endpoint is exposed; its database component checks a
real connection without exposing connection details.

## Run Java locally

Requires JDK 21 and Maven 3.6.3 or newer. Start PostgreSQL with Compose, or use
an existing PostgreSQL server with a database and user already created:

```shell
docker compose up -d --wait postgres
```

For the default database settings:

```shell
mvn spring-boot:run
```

For custom settings, PowerShell example:

```powershell
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/hospital_db'
$env:DATABASE_USERNAME = 'hospital'
$env:DATABASE_PASSWORD = 'hospital'
$env:THYMELEAF_CACHE = 'false'
mvn spring-boot:run
```

If the Compose application is already running, stop only that service with
`docker compose stop app`, or set a different `SERVER_PORT` for the local app.

To package and run the executable JAR:

```shell
mvn clean verify
java -jar target/hospital-management-system.jar
```

## Verification

```shell
mvn clean verify
```

The automated suite checks domain updates, rejected changes, and shared behavior
through the `Person` abstraction. JPA tests verify inherited identity and fields,
timestamp callbacks, enum storage, and inherited Bean Validation using test-only
H2. These persistence tests roll back their transactions.

The directory tests exercise complete create/edit/view/search/delete flows,
validation failures without writes, combined filters, pagination, code stability,
existing-record code initialization, escaped text, protected form binding, and
friendly errors. They roll back their data after each test.

Appointment tests cover booking and status flows, validation, conflicts, slot
reuse after cancellation, completed-record protection, stale edits, combined
filters, pagination, linked history, and deletion protection. A concurrency test
starts two independent booking transactions together and requires exactly one
successful reservation. Test fixtures are rolled back or explicitly cleaned up.

Billing tests cover invoice arithmetic and field binding, all four payment methods,
partial and full settlement, zero totals, invalid amounts and references, duplicate
appointment bills, patient ownership, immutable invoice details, deletion protection,
filters, pagination, and printable invoice rendering. Independent concurrent
transactions verify one invoice per appointment and one payment per submitted bill
version. Rendering is also checked outside service transactions with open-in-view
disabled, and committed test records are explicitly cleaned up.

Dashboard tests verify empty summaries, status/date counts, exact revenue and
outstanding balance calculations with multiple partial payments, updates after
payments and status transitions, bounded recent lists with deterministic ordering,
escaped patient names, filtered links, and rendering after the service transaction
closes. Clock tests cover the Asia/Dhaka midnight boundary and timezone configuration.

Demo data tests start a separate context with seeding enabled and its own
in-memory database. They check the blueprint's record counts, generated codes,
every appointment status, specialization, and payment method, conflict-free
doctor and patient slots, invoice arithmetic and ownership, the populated
dashboard totals and pages, and that seeding an already populated database
changes nothing. The default context is also checked to confirm that no demo
records are seeded unless they are requested.

The web tests start the full Spring context with a test-only H2 database
and check both home routes, rendered Thymeleaf fragments, local CSS/JS resources,
database-aware health, and the absence of the configuration endpoint. H2 is never
included in the runtime JAR. These tests also run in `docker compose build app`;
no local Java installation or running database is required for that build.

For a real PostgreSQL smoke check after Compose starts (adjust the user and
database names if you customized them):

```shell
docker compose exec postgres psql -U hospital -d hospital_db -c "SELECT current_database(), current_user;"
docker compose exec postgres psql -U hospital -d hospital_db -c "\d patients"
docker compose exec postgres psql -U hospital -d hospital_db -c "\d doctors"
docker compose exec postgres psql -U hospital -d hospital_db -c "\d appointments"
docker compose exec postgres psql -U hospital -d hospital_db -c "\d bills"
docker compose exec postgres psql -U hospital -d hospital_db -c "\d payments"
```

The patient and doctor tables should contain `id`, `created_at`, `updated_at`, `first_name`,
`last_name`, `phone`, and `email`, alongside the subclass-specific fields.
The registration code columns have unique constraints. The tables start empty;
register records through the application to populate them, or start once with
`DEMO_DATA=true` to add the [Phase 11](#phase-11-demo-data) demo records.

On PowerShell, the HTTP checks are:

```powershell
(Invoke-WebRequest -UseBasicParsing http://localhost:8080).StatusCode
Invoke-RestMethod http://localhost:8080/actuator/health
```

Expect HTTP 200 and database status `UP`. Check the page at a narrow browser
width: navigation should collapse, cards should stack, and all styling and scripts
should load locally. Dashboard cards link to their directories or filtered lists.

To check health failure and recovery in your local setup, stop PostgreSQL with
`docker compose stop postgres`. The health endpoint should return HTTP 503 with
database status `DOWN`. Restart it with `docker compose start postgres`; health
should recover to `UP` once a connection is available.

Framework references: [Spring Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html),
[Actuator health](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html),
and [Bootstrap documentation](https://getbootstrap.com/docs/5.3/getting-started/introduction/).
