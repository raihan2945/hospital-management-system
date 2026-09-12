# Hospital Management System
## Java + Spring Boot + PostgreSQL — Varsity Assignment Project Blueprint

> **Project Type:** Web-based Hospital Management System  
> **Primary Language:** Java  
> **Backend / Web Framework:** Spring Boot  
> **UI:** Thymeleaf + Bootstrap + JavaScript  
> **Database:** PostgreSQL  
> **Build Tool:** Maven  
> **Containerization:** Docker + Docker Compose  
> **Recommended Java Version:** Java 21 LTS  
> **Architecture:** Layered Monolithic Architecture  
> **Development Style:** Agentic AI assisted, phase-by-phase

---

# 1. Project Overview

The **Hospital Management System (HMS)** is a web application for managing the basic operational activities of a small hospital or clinic.

The system will support:

- Patient management
- Doctor management
- Appointment management
- Billing management
- Dashboard and summary information
- Search, filtering, validation, and status management

The project is intentionally designed to demonstrate the major **Object-Oriented Programming (OOP)** concepts required for the varsity assignment:

- Encapsulation
- Abstraction
- Inheritance
- Polymorphism

This project is appropriate for the assignment because it contains meaningful real-world functionalities while remaining small enough to complete within a university project timeline.

---

# 2. Main Project Objective

The objective is to develop an interactive Hospital Management System where hospital staff can:

1. Register and manage patients.
2. Register and manage doctors.
3. Schedule and manage appointments.
4. Generate and manage patient bills.
5. View hospital activity from a dashboard.
6. Search and filter data efficiently.
7. Demonstrate correct use of Java and OOP principles.
8. Store application data permanently in PostgreSQL.
9. Run the complete application using Docker Compose.

---

# 3. Recommended Technology Stack

| Layer | Technology |
|---|---|
| Programming Language | Java 21 |
| Framework | Spring Boot |
| Web | Spring MVC |
| UI Template Engine | Thymeleaf |
| UI Styling | Bootstrap 5 |
| Client Interaction | JavaScript |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Validation | Jakarta Bean Validation |
| Build Tool | Maven |
| Object Mapping | Manual mapping or MapStruct optional |
| Database Migration | Flyway optional but recommended |
| Containerization | Docker |
| Local Orchestration | Docker Compose |
| Version Control | Git + GitHub |

---

# 4. Why This Stack?

Spring Boot makes the application easier to organize into clean layers while still allowing the project to demonstrate core Java concepts.

Using **Thymeleaf + Bootstrap** keeps the project Java-centered because UI pages are rendered through the Spring Boot application.

This is simpler for a varsity assignment than maintaining a separate frontend framework such as Next.js or React.

The proposed structure also makes Docker deployment simple because only the following services are required:

- Spring Boot application
- PostgreSQL database

---

# 5. Functional Scope

The first version should contain four major modules:

1. Patient Management
2. Doctor Management
3. Appointment Management
4. Billing Management

Additional dashboard functionality will connect these modules.

---

# 6. User Roles

For the first version, use a simple hospital staff/admin user.

### Optional Future Roles

- Admin
- Receptionist
- Doctor
- Accountant

Authentication and authorization can be added later if your assignment timeline allows it.

For the core assignment, it is acceptable to focus primarily on OOP and CRUD/business functionality.

---

# 7. Core Functional Requirements

## 7.1 Dashboard

The dashboard should display:

- Total patients
- Total doctors
- Total appointments
- Today's appointments
- Pending appointments
- Completed appointments
- Total bills
- Total collected amount
- Recent patients
- Recent appointments

---

# 8. Patient Management

The system must allow staff to:

- Register a patient
- View patient list
- View patient details
- Update patient information
- Delete/deactivate a patient
- Search patients
- Filter patients
- View appointment history
- View billing history

### Patient Information

Suggested fields:

```text
id
patientCode
firstName
lastName
gender
dateOfBirth
phone
email
address
bloodGroup
emergencyContact
createdAt
updatedAt
```

### Example Patient Code

```text
PAT-000001
PAT-000002
```

---

# 9. Doctor Management

The system must allow staff to:

- Register a doctor
- View doctors
- View doctor details
- Update doctor
- Deactivate/delete doctor
- Search doctors
- Filter by specialization
- View doctor's scheduled appointments

### Doctor Information

```text
id
doctorCode
firstName
lastName
phone
email
specialization
qualification
consultationFee
available
createdAt
updatedAt
```

### Example Doctor Code

```text
DOC-000001
DOC-000002
```

### Suggested Specializations

- General Medicine
- Cardiology
- Neurology
- Pediatrics
- Dermatology
- Orthopedics
- ENT
- Gynecology

---

# 10. Appointment Management

The system must allow staff to:

- Create an appointment
- Select patient
- Select doctor
- Select date
- Select appointment time
- Add symptoms/reason
- Update appointment
- Cancel appointment
- Complete appointment
- View appointment list
- Filter by date
- Filter by doctor
- Filter by patient
- Filter by status

### Appointment Information

```text
id
appointmentCode
patient
doctor
appointmentDate
appointmentTime
reason
status
notes
createdAt
updatedAt
```

### Appointment Status

```text
SCHEDULED
CONFIRMED
COMPLETED
CANCELLED
```

### Important Business Rules

1. Appointment date cannot be empty.
2. Appointment time cannot be empty.
3. Patient must exist.
4. Doctor must exist.
5. A doctor cannot have two active appointments at the same date and time.
6. Cancelled appointments should not be treated as active appointments.
7. Completed appointments should not normally be editable.

---

# 11. Billing Management

The billing module should allow staff to:

- Create a bill for a patient
- Optionally connect the bill to an appointment
- Add consultation charge
- Add service charge
- Add medicine/other charge
- Apply discount
- Calculate total automatically
- Record payment
- Update payment status
- View bill history
- Print/view invoice

### Billing Information

```text
id
invoiceNumber
patient
appointment
consultationFee
serviceCharge
medicineCharge
otherCharge
discount
subtotal
totalAmount
paidAmount
dueAmount
paymentStatus
paymentMethod
createdAt
updatedAt
```

### Payment Status

```text
UNPAID
PARTIALLY_PAID
PAID
```

### Payment Method

```text
CASH
CARD
MOBILE_BANKING
BANK_TRANSFER
```

### Billing Formula

```text
subtotal =
    consultationFee
    + serviceCharge
    + medicineCharge
    + otherCharge

totalAmount =
    subtotal - discount

dueAmount =
    totalAmount - paidAmount
```

---

# 12. Non-Functional Requirements

The system should provide:

- Responsive UI
- Proper input validation
- Clear error messages
- Clean Java package structure
- Reusable services
- Data persistence
- Database constraints
- Simple navigation
- Search and filtering
- Proper exception handling
- Docker-based setup
- Maintainable source code

---

# 13. OOP Requirements

This section is especially important for the varsity evaluation.

The application must intentionally demonstrate the following OOP principles.

---

# 14. Encapsulation

Encapsulation means keeping object state protected and exposing controlled methods for accessing or modifying that state.

Example:

```java
public class Patient {

    private Long id;
    private String firstName;
    private String lastName;
    private String phone;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void updatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone cannot be empty");
        }

        this.phone = phone;
    }
}
```

Private fields demonstrate encapsulation.

Instead of directly modifying important state from anywhere, domain/service methods can validate changes.

---

# 15. Abstraction

Abstraction can be demonstrated through interfaces and abstract classes.

Example service abstraction:

```java
public interface BillingCalculator {

    BigDecimal calculateTotal(Bill bill);
}
```

Implementation:

```java
@Service
public class StandardBillingCalculator implements BillingCalculator {

    @Override
    public BigDecimal calculateTotal(Bill bill) {
        BigDecimal subtotal =
                bill.getConsultationFee()
                .add(bill.getServiceCharge())
                .add(bill.getMedicineCharge())
                .add(bill.getOtherCharge());

        return subtotal.subtract(bill.getDiscount());
    }
}
```

The caller depends on the abstraction instead of the implementation.

---

# 16. Inheritance

Create a common base class for shared information.

Example:

```java
@MappedSuperclass
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String phone;
    private String email;
}
```

Then:

```java
@Entity
public class Patient extends Person {

    private LocalDate dateOfBirth;
    private String bloodGroup;
}
```

```java
@Entity
public class Doctor extends Person {

    private String specialization;
    private BigDecimal consultationFee;
}
```

This clearly demonstrates inheritance.

---

# 17. Polymorphism

A very good place to demonstrate polymorphism is payment processing.

Create:

```java
public interface PaymentProcessor {

    PaymentResult process(Bill bill, BigDecimal amount);
}
```

Implement multiple payment processors:

```java
public class CashPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Bill bill, BigDecimal amount) {
        // cash payment logic
        return new PaymentResult(true, "Cash payment recorded");
    }
}
```

```java
public class CardPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Bill bill, BigDecimal amount) {
        // simulated card payment logic
        return new PaymentResult(true, "Card payment recorded");
    }
}
```

```java
public class MobileBankingPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Bill bill, BigDecimal amount) {
        // simulated mobile payment logic
        return new PaymentResult(true, "Mobile banking payment recorded");
    }
}
```

All implementations can be handled through:

```java
PaymentProcessor processor;
processor.process(bill, amount);
```

This demonstrates runtime polymorphism.

---

# 18. OOP Demonstration Summary

| OOP Concept | Project Example |
|---|---|
| Encapsulation | Private entity/domain fields and controlled update methods |
| Abstraction | Service interfaces such as `BillingCalculator` |
| Inheritance | `Patient` and `Doctor` extending `Person` |
| Polymorphism | Different `PaymentProcessor` implementations |

During the final presentation, explicitly explain these four examples.

---

# 19. Recommended Architecture

Use a traditional layered architecture.

```text
Browser
   |
   v
Controller Layer
   |
   v
Service Layer
   |
   v
Repository Layer
   |
   v
PostgreSQL
```

Responsibilities:

### Controller Layer

Responsible for:

- Receiving HTTP requests
- Validating form input
- Calling service methods
- Sending data to Thymeleaf templates
- Redirecting users

### Service Layer

Responsible for:

- Business rules
- Validation beyond form validation
- Appointment conflict checking
- Billing calculation
- Payment processing
- Transaction management

### Repository Layer

Responsible for:

- Database queries
- CRUD operations
- Search operations

### Entity / Domain Layer

Responsible for representing:

- Patient
- Doctor
- Appointment
- Bill
- Payment

---

# 20. Suggested Project Structure

```text
hospital-management-system/
│
├── docker-compose.yml
├── .env.example
├── README.md
├── pom.xml
├── Dockerfile
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/hms/
│   │   │       │
│   │   │       ├── HospitalManagementApplication.java
│   │   │       │
│   │   │       ├── config/
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   ├── DashboardController.java
│   │   │       │   ├── PatientController.java
│   │   │       │   ├── DoctorController.java
│   │   │       │   ├── AppointmentController.java
│   │   │       │   └── BillingController.java
│   │   │       │
│   │   │       ├── domain/
│   │   │       │   ├── Person.java
│   │   │       │   ├── Patient.java
│   │   │       │   ├── Doctor.java
│   │   │       │   ├── Appointment.java
│   │   │       │   ├── Bill.java
│   │   │       │   ├── Payment.java
│   │   │       │   └── enums/
│   │   │       │
│   │   │       ├── dto/
│   │   │       │
│   │   │       ├── repository/
│   │   │       │
│   │   │       ├── service/
│   │   │       │   ├── PatientService.java
│   │   │       │   ├── DoctorService.java
│   │   │       │   ├── AppointmentService.java
│   │   │       │   ├── BillingService.java
│   │   │       │   └── payment/
│   │   │       │
│   │   │       ├── exception/
│   │   │       │
│   │   │       └── util/
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       │
│   │       ├── templates/
│   │       │   ├── layout/
│   │       │   ├── dashboard/
│   │       │   ├── patients/
│   │       │   ├── doctors/
│   │       │   ├── appointments/
│   │       │   └── billing/
│   │       │
│   │       ├── static/
│   │       │   ├── css/
│   │       │   ├── js/
│   │       │   └── images/
│   │       │
│   │       └── db/
│   │           └── migration/
│   │
│   └── test/
│       └── java/
│
└── docs/
    ├── SRS.md
    ├── database-design.md
    └── development-plan.md
```

---

# 21. Suggested Domain Model

```text
Person
 ├── Patient
 └── Doctor

Patient
 ├── appointments
 └── bills

Doctor
 └── appointments

Appointment
 ├── patient
 ├── doctor
 └── optional bill

Bill
 ├── patient
 ├── optional appointment
 └── payments
```

---

# 22. Database Relationships

## Patient → Appointment

```text
Patient 1 ------ * Appointment
```

One patient can have many appointments.

## Doctor → Appointment

```text
Doctor 1 ------ * Appointment
```

One doctor can have many appointments.

## Patient → Bill

```text
Patient 1 ------ * Bill
```

One patient can have many bills.

## Appointment → Bill

```text
Appointment 1 ------ 0..1 Bill
```

An appointment may have one bill.

## Bill → Payment

```text
Bill 1 ------ * Payment
```

A bill can receive multiple payments.

---

# 23. Suggested Database Tables

## patients

```text
id
patient_code
first_name
last_name
gender
date_of_birth
phone
email
address
blood_group
emergency_contact
created_at
updated_at
```

## doctors

```text
id
doctor_code
first_name
last_name
phone
email
specialization
qualification
consultation_fee
available
created_at
updated_at
```

## appointments

```text
id
appointment_code
patient_id
doctor_id
appointment_date
appointment_time
reason
status
notes
created_at
updated_at
```

## bills

```text
id
invoice_number
patient_id
appointment_id
consultation_fee
service_charge
medicine_charge
other_charge
discount
subtotal
total_amount
paid_amount
due_amount
payment_status
created_at
updated_at
```

## payments

```text
id
bill_id
amount
payment_method
transaction_reference
payment_date
created_at
```

---

# 24. Suggested Unique Constraints

Use database constraints where appropriate.

Examples:

```text
patients.patient_code UNIQUE
doctors.doctor_code UNIQUE
appointments.appointment_code UNIQUE
bills.invoice_number UNIQUE
```

For appointment conflict prevention:

```text
doctor_id + appointment_date + appointment_time
```

However, because cancelled appointments may reuse a slot, application-level conflict validation is usually easier for this assignment.

---

# 25. UI Requirements

Use Bootstrap to create an interactive and professional layout.

Recommended layout:

```text
+-----------------------------------------------------+
| Logo        Hospital Management System       User   |
+-------------+---------------------------------------+
| Sidebar     | Dashboard                             |
|             |                                       |
| Dashboard   | Cards                                 |
| Patients    | Charts / tables                       |
| Doctors     | Recent appointments                   |
| Appointments|                                       |
| Billing     |                                       |
+-------------+---------------------------------------+
```

---

# 26. Dashboard UI

Dashboard cards:

```text
Total Patients
Total Doctors
Today's Appointments
Pending Appointments
Total Revenue
Outstanding Due
```

Optional charts:

- Appointment status chart
- Monthly revenue chart

Charts can be implemented with Chart.js if desired.

---

# 27. Patient Pages

Recommended pages:

```text
/patients
/patients/new
/patients/{id}
/patients/{id}/edit
```

UI should contain:

- Search bar
- Add patient button
- Data table
- Pagination
- Edit button
- View button
- Delete/deactivate button

---

# 28. Doctor Pages

Recommended pages:

```text
/doctors
/doctors/new
/doctors/{id}
/doctors/{id}/edit
```

Include:

- Doctor profile information
- Specialization
- Consultation fee
- Availability
- Upcoming appointment list

---

# 29. Appointment Pages

Recommended pages:

```text
/appointments
/appointments/new
/appointments/{id}
/appointments/{id}/edit
```

Appointment form:

```text
Patient
Doctor
Date
Time
Reason
Notes
```

Filters:

```text
Date
Doctor
Status
Patient
```

---

# 30. Billing Pages

Recommended pages:

```text
/billing
/billing/new
/billing/{id}
/billing/{id}/payment
```

Invoice page should display:

```text
Hospital Name
Invoice Number
Patient
Appointment
Charges
Discount
Total
Paid
Due
Payment Status
```

Provide a Print button using:

```javascript
window.print();
```

---

# 31. Validation Requirements

Examples:

### Patient

```text
First name required
Last name required
Phone required
Email format valid
Date of birth cannot be in future
```

### Doctor

```text
Name required
Specialization required
Consultation fee >= 0
```

### Appointment

```text
Patient required
Doctor required
Date required
Time required
Cannot double-book doctor
```

### Bill

```text
Charges cannot be negative
Discount cannot be negative
Paid amount cannot be negative
Paid amount should not exceed permitted business rule
```

---

# 32. Exception Handling

Create custom exceptions.

Examples:

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

```java
public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException(String message) {
        super(message);
    }
}
```

```java
public class InvalidPaymentException extends RuntimeException {
    public InvalidPaymentException(String message) {
        super(message);
    }
}
```

Create centralized exception handling where appropriate.

---

# 33. Required Maven Dependencies

Typical dependencies:

```xml
<dependencies>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>

</dependencies>
```

Optional:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

---

# 34. Spring Configuration

Create:

```text
src/main/resources/application.yml
```

Example:

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: hospital-management-system

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/hospital_db}
    username: ${DATABASE_USERNAME:hospital}
    password: ${DATABASE_PASSWORD:hospital}

  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:update}
    properties:
      hibernate:
        format_sql: true
    open-in-view: false

  thymeleaf:
    cache: false

logging:
  level:
    org.hibernate.SQL: INFO
```

For a production-style setup, prefer Flyway migrations and:

```text
ddl-auto=validate
```

For a varsity project, using `update` during early development is acceptable.

---

# 35. Environment Variables

Create:

```text
.env.example
```

Contents:

```env
SERVER_PORT=8080

POSTGRES_DB=hospital_db
POSTGRES_USER=hospital
POSTGRES_PASSWORD=hospital

DATABASE_URL=jdbc:postgresql://postgres:5432/hospital_db
DATABASE_USERNAME=hospital
DATABASE_PASSWORD=hospital

JPA_DDL_AUTO=update
```

Copy it locally:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

---

# 36. Dockerfile

Create a file named:

```text
Dockerfile
```

Recommended multi-stage Dockerfile:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipChecks


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

# 37. Docker Compose Setup

Create:

```text
docker-compose.yml
```

Example:

```yaml
services:

  postgres:
    image: postgres:17
    container_name: hms-postgres

    environment:
      POSTGRES_DB: ${POSTGRES_DB:-hospital_db}
      POSTGRES_USER: ${POSTGRES_USER:-hospital}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-hospital}

    ports:
      - "5432:5432"

    volumes:
      - hospital_postgres_data:/var/lib/postgresql/data

    healthcheck:
      test:
        [
          "CMD-SHELL",
          "pg_isready -U ${POSTGRES_USER:-hospital} -d ${POSTGRES_DB:-hospital_db}"
        ]
      interval: 5s
      timeout: 5s
      retries: 10


  app:
    build:
      context: .
      dockerfile: Dockerfile

    container_name: hms-app

    depends_on:
      postgres:
        condition: service_healthy

    environment:
      SERVER_PORT: 8080
      DATABASE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-hospital_db}
      DATABASE_USERNAME: ${POSTGRES_USER:-hospital}
      DATABASE_PASSWORD: ${POSTGRES_PASSWORD:-hospital}
      JPA_DDL_AUTO: update

    ports:
      - "8080:8080"

    restart: unless-stopped


volumes:
  hospital_postgres_data:
```

---

# 38. Run With Docker Compose

Build and start:

```bash
docker compose up --build
```

Run in background:

```bash
docker compose up --build -d
```

View logs:

```bash
docker compose logs -f app
```

Stop:

```bash
docker compose down
```

Stop and remove database volume:

```bash
docker compose down -v
```

After startup, open:

```text
http://localhost:8080
```

---

# 39. Using a Remote PostgreSQL Database

If you already have a remote PostgreSQL database, the PostgreSQL Docker service is not required.

Your `.env` can contain:

```env
DATABASE_URL=jdbc:postgresql://your-db-host:5432/hospital_db
DATABASE_USERNAME=your_username
DATABASE_PASSWORD=your_password
```

Then a simplified Compose file can be used:

```yaml
services:

  app:
    build:
      context: .
      dockerfile: Dockerfile

    environment:
      SERVER_PORT: 8080
      DATABASE_URL: ${DATABASE_URL}
      DATABASE_USERNAME: ${DATABASE_USERNAME}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      JPA_DDL_AUTO: ${JPA_DDL_AUTO:-update}

    ports:
      - "8080:8080"

    restart: unless-stopped
```

---

# 40. Running Without Docker

Requirements:

- Java 21
- Maven
- PostgreSQL

Run:

```bash
mvn spring-boot:run
```

Or package:

```bash
mvn clean package
```

Then:

```bash
java -jar target/*.jar
```

---

# 41. Git Repository Setup

Initialize:

```bash
git init
```

Create `.gitignore`:

```gitignore
target/
.idea/
.vscode/
.env
*.iml
.DS_Store
```

Commit:

```bash
git add .
git commit -m "Initial hospital management system setup"
```

---

# 42. Development Strategy

Do not ask the AI agent to build the whole system in a single prompt.

Develop the application in phases.

Each phase should:

1. Have a clear goal.
2. Produce working code.
3. Be tested.
4. Be committed to Git.
5. Be reviewed before continuing.

Recommended process:

```text
Plan
  ↓
Generate
  ↓
Review
  ↓
Run
  ↓
Test
  ↓
Fix
  ↓
Commit
  ↓
Next Phase
```

---

# 43. Full Development Phases

## Phase 0 — Planning

### Goal

Finalize project requirements before writing code.

### Tasks

- Confirm modules
- Confirm OOP mapping
- Define entities
- Define relationships
- Define UI pages
- Define business rules
- Define package structure
- Create Git repository

### Deliverables

```text
README.md
SRS.md
database-design.md
development-plan.md
```

### Completion Criteria

You should be able to explain the complete project architecture before implementation starts.

---

# 44. Phase 1 — Spring Boot Project Initialization

### Goal

Create the base Java project.

### Tasks

- Create Spring Boot project
- Configure Maven
- Add dependencies
- Configure application.yml
- Create base package structure
- Add PostgreSQL configuration
- Add simple home page
- Verify application startup

### Deliverables

```text
pom.xml
application.yml
HospitalManagementApplication.java
HomeController.java
home.html
```

---

# 45. Phase 2 — Docker Setup

### Goal

Make the project executable without installing PostgreSQL manually.

### Tasks

- Create Dockerfile
- Create docker-compose.yml
- Create `.env.example`
- Start PostgreSQL
- Start application
- Verify database connection

---

# 46. Phase 3 — Common Domain / OOP Foundation

### Goal

Implement shared object-oriented architecture.

### Tasks

Create:

```text
Person.java
BaseEntity.java optional
Gender.java
```

Implement inheritance:

```text
Person
 ├── Patient
 └── Doctor
```

### OOP Focus

- Encapsulation
- Inheritance

---

# 47. Phase 4 — Patient Management

### Goal

Complete patient CRUD.

### Backend

Implement:

```text
Patient entity
PatientRepository
PatientService
PatientController
Patient form DTO
```

### UI

Implement:

```text
Patient list
Create patient
Edit patient
View patient
Delete/deactivate patient
Search patient
```

### Git Commit

```text
feat: implement patient management
```

---

# 48. Phase 5 — Doctor Management

### Goal

Complete doctor CRUD.

### Backend

Implement:

```text
Doctor entity
DoctorRepository
DoctorService
DoctorController
Doctor form DTO
```

### UI

Implement:

```text
Doctor list
Create doctor
Edit doctor
Doctor details
Specialization filter
Availability field
```

### Git Commit

```text
feat: implement doctor management
```

---

# 49. Phase 6 — Appointment Management

### Goal

Connect patients and doctors through appointments.

### Backend

Create:

```text
Appointment
AppointmentStatus
AppointmentRepository
AppointmentService
AppointmentController
```

### Important Business Logic

Implement:

```text
isDoctorAvailable(doctorId, date, time)
```

Pseudo-code:

```java
if (appointmentRepository
        .existsActiveAppointment(doctorId, date, time)) {

    throw new AppointmentConflictException(
        "Doctor already has an appointment at this time"
    );
}
```

### UI

Implement:

```text
Appointment list
New appointment
Edit appointment
Cancel appointment
Complete appointment
Filters
```

### Git Commit

```text
feat: implement appointment management
```

---

# 50. Phase 7 — Billing Management

### Goal

Implement bill calculation and invoice management.

### Backend

Create:

```text
Bill
Payment
PaymentStatus
PaymentMethod
BillingCalculator
StandardBillingCalculator
BillingService
BillingController
```

### OOP Focus

This phase should strongly demonstrate:

- Abstraction
- Polymorphism

### Payment Implementations

```text
CashPaymentProcessor
CardPaymentProcessor
MobileBankingPaymentProcessor
```

### UI

Implement:

```text
Billing list
Create bill
View invoice
Record payment
Print invoice
```

### Git Commit

```text
feat: implement billing and payment management
```

---

# 51. Phase 8 — Dashboard

### Goal

Create interactive system overview.

### Backend

Create dashboard service queries:

```text
countPatients()
countDoctors()
countTodayAppointments()
countPendingAppointments()
calculateTotalRevenue()
calculateOutstandingDue()
```

### UI

Use Bootstrap cards.

Optional Chart.js charts:

```text
Appointment Status
Monthly Revenue
```

### Git Commit

```text
feat: implement dashboard
```

---

# 52. Phase 9 — UI/UX Improvement

### Goal

Make the project visually presentable.

### Tasks

- Add navigation bar
- Add sidebar
- Add Bootstrap icons
- Add responsive tables
- Add badges for status
- Add toast/alert messages
- Add confirmation modal
- Add empty-state messages
- Improve forms
- Improve mobile responsiveness

### Example Status Colors

```text
SCHEDULED -> blue
CONFIRMED -> info
COMPLETED -> green
CANCELLED -> red

PAID -> green
PARTIALLY_PAID -> orange
UNPAID -> red
```

---

# 53. Phase 10 — Validation and Exception Handling

### Goal

Make application behavior reliable.

### Tasks

- Add Bean Validation
- Create custom exceptions
- Add duplicate checks
- Add global error handling
- Add user-friendly validation messages

---

# 54. Phase 11 — Seed / Demo Data

### Goal

Make project demonstration easy.

Create example:

```text
5 doctors
10 patients
10 appointments
5 bills
```

Options:

- `data.sql`
- CommandLineRunner
- Flyway migration

Demo data helps greatly during the final presentation.

---

# 55. Phase 12 — Documentation

Prepare:

```text
README.md
SRS.md
ER diagram
Class diagram
Screenshots
Installation instructions
OOP explanation
Testing explanation
```

README should contain:

```text
Project description
Features
Technologies
Architecture
OOP concepts
Installation
Docker instructions
Screenshots
Future improvements
```

---

# 56. Phase 13 — Final Review

Before submission verify:

```text
[ ] Application builds
[ ] Docker Compose works
[ ] PostgreSQL connects
[ ] Patient CRUD works
[ ] Doctor CRUD works
[ ] Appointment works
[ ] Double booking prevented
[ ] Billing works
[ ] Payment works
[ ] Dashboard works
[ ] UI is responsive
[ ] Validation works
[ ] OOP concepts are clearly implemented
[ ] README is complete
[ ] Demo data is available
```

---

# 57. Suggested Agentic AI Workflow

Because the application will be developed with an agentic AI assistant, give the agent a strict development policy.

Create:

```text
AGENTS.md
```

Suggested content:

```markdown
# Agent Instructions

You are developing a varsity Hospital Management System.

## Stack

- Java 21
- Spring Boot
- Spring MVC
- Thymeleaf
- Bootstrap
- Spring Data JPA
- PostgreSQL
- Maven
- Docker

## Development Rules

1. Follow layered architecture.
2. Do not put business logic in controllers.
3. Use DTO/form classes for form validation.
4. Keep entities focused on domain state.
5. Use meaningful names.
6. Use constructor injection.
7. Avoid unnecessary dependencies.
8. Implement one module at a time.
9. Do not modify unrelated files.
10. Keep code simple enough for a varsity presentation.
11. Clearly demonstrate encapsulation, abstraction, inheritance, and polymorphism.
12. Do not over-engineer the application.
13. Before coding, inspect existing project conventions.
14. After each phase, summarize changed files and how to verify them manually.
```

---

# 58. Recommended Agent Prompt Template

For each development phase, use a prompt similar to:

```text
We are developing a Hospital Management System for a varsity Java OOP
assignment.

Read AGENTS.md and the existing repository first.

Current phase:
Patient Management.

Requirements:

- Implement Patient entity.
- Patient must inherit from Person.
- Add repository.
- Add service layer.
- Add controller.
- Add form DTO validation.
- Add Thymeleaf pages.
- Add Bootstrap styling.
- Add patient search.

Constraints:

- Java 21
- Spring Boot
- PostgreSQL
- Maven
- No business logic inside controllers.
- Use constructor injection.
- Keep implementation simple and explainable.
- Do not work on Doctor, Appointment, or Billing yet.

After implementation:

1. Explain the architecture.
2. List all changed files.
3. Explain how to verify the module manually.
```

---

# 59. Important Rule for Agentic Development

Do **not** use prompts like:

```text
Build the full hospital management system.
```

That usually creates:

- inconsistent architecture
- duplicated code
- broken relationships
- unnecessary complexity
- difficult debugging

Instead use focused phases.

Recommended:

```text
Phase 1
Project setup

Phase 2
Docker

Phase 3
OOP base model

Phase 4
Patients

Phase 5
Doctors

Phase 6
Appointments

Phase 7
Billing

Phase 8
Dashboard

Phase 9
UI refinement

Phase 10
Documentation
```

---

# 60. Suggested Class Diagram

```text
                +----------------------+
                |       Person         |
                +----------------------+
                | id                   |
                | firstName            |
                | lastName             |
                | phone                |
                | email                |
                +----------------------+
                   ^              ^
                   |              |
          +--------+              +---------+
          |                               |
+----------------------+      +----------------------+
|       Patient        |      |        Doctor        |
+----------------------+      +----------------------+
| patientCode          |      | doctorCode           |
| dateOfBirth          |      | specialization       |
| bloodGroup           |      | consultationFee      |
+----------------------+      +----------------------+
          |                               |
          |                               |
          +-----------+       +-----------+
                      |       |
                +----------------------+
                |     Appointment      |
                +----------------------+
                | appointmentCode      |
                | date                 |
                | time                 |
                | status               |
                +----------------------+
                         |
                         |
                         v
                +----------------------+
                |        Bill          |
                +----------------------+
                | invoiceNumber        |
                | subtotal             |
                | discount             |
                | totalAmount          |
                | paidAmount           |
                | dueAmount            |
                +----------------------+
                         |
                         |
                         v
                +----------------------+
                |       Payment        |
                +----------------------+
                | amount               |
                | paymentMethod        |
                | paymentDate          |
                +----------------------+
```

---

# 61. Suggested Service Interfaces

Example:

```java
public interface PatientService {

    Patient createPatient(PatientForm form);

    Patient updatePatient(Long id, PatientForm form);

    Patient getPatient(Long id);

    List<Patient> searchPatients(String keyword);

    void deletePatient(Long id);
}
```

Appointment:

```java
public interface AppointmentService {

    Appointment createAppointment(AppointmentForm form);

    Appointment updateAppointment(Long id, AppointmentForm form);

    void cancelAppointment(Long id);

    void completeAppointment(Long id);

    boolean isDoctorAvailable(
        Long doctorId,
        LocalDate date,
        LocalTime time
    );
}
```

Billing:

```java
public interface BillingService {

    Bill createBill(CreateBillForm form);

    Payment recordPayment(
        Long billId,
        BigDecimal amount,
        PaymentMethod paymentMethod
    );

    Bill getBill(Long billId);
}
```

These interfaces contribute to abstraction.

---

# 62. Sample Appointment Conflict Repository Query

Conceptual Spring Data method:

```java
boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
    Long doctorId,
    LocalDate appointmentDate,
    LocalTime appointmentTime,
    AppointmentStatus status
);
```

Then:

```java
boolean conflict = appointmentRepository
    .existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
        doctorId,
        date,
        time,
        AppointmentStatus.CANCELLED
    );
```

For a student project this is simple and easy to explain.

---

# 63. ID Generation Strategy

Use database IDs internally:

```text
1
2
3
```

But display user-friendly codes:

```text
PAT-000001
DOC-000001
APT-000001
INV-000001
```

You can generate them after entity creation based on the database ID, or use another simple sequence strategy.

Do not build an unnecessarily complex distributed ID system for this project.

---

# 64. Security Scope

Authentication is optional for the initial assignment unless specifically required.

If added later, use:

```text
Spring Security
```

Possible users:

```text
ADMIN
RECEPTIONIST
ACCOUNTANT
```

Suggested access:

```text
ADMIN
Everything

RECEPTIONIST
Patients
Doctors
Appointments

ACCOUNTANT
Billing
Payments
```

Add security only after the core modules work.

---

# 65. Optional Features

Only add these if the required project is already stable:

- Login
- Role-based access
- Medical records
- Prescription management
- Room/bed management
- Department management
- Medicine inventory
- PDF invoice
- Email notifications
- Appointment calendar
- Audit log
- CSV export
- Dark mode
- Advanced analytics

Do not allow optional functionality to prevent completion of the main assignment.

---

# 66. Minimum Viable Submission

If time becomes limited, the minimum acceptable version should include:

```text
Patient CRUD
Doctor CRUD
Appointment CRUD
Doctor conflict prevention
Billing calculation
Payment status
PostgreSQL persistence
Interactive Bootstrap UI
Docker setup
OOP demonstration
README
```

This is already a meaningful Java OOP project.

---

# 67. Recommended Final Demo Flow

During presentation:

### Step 1

Show dashboard.

### Step 2

Create a new patient.

### Step 3

Create/select doctor.

### Step 4

Create appointment.

### Step 5

Try creating another appointment for the same doctor/time.

Show conflict validation.

### Step 6

Complete appointment.

### Step 7

Generate bill.

### Step 8

Record partial payment.

### Step 9

Record remaining payment.

### Step 10

Show invoice as PAID.

### Step 11

Explain OOP implementation.

Explain:

```text
Person -> Patient/Doctor
```

for inheritance.

Explain private state for encapsulation.

Explain:

```text
BillingCalculator
```

for abstraction.

Explain:

```text
PaymentProcessor
```

implementations for polymorphism.

This gives you a strong final presentation because you demonstrate both application functionality and theoretical Java concepts.

---

# 68. Presentation Talking Points

You can explain the system using:

```text
This project is a web-based Hospital Management System implemented
using Java and Spring Boot.

PostgreSQL is used for permanent data storage.

The application follows a layered architecture consisting of
controllers, services, repositories, and domain entities.

The system provides patient, doctor, appointment, and billing
management.

The project demonstrates the four major OOP principles.

Encapsulation is implemented using private state and controlled
domain/service operations.

Inheritance is implemented using the common Person base class,
extended by Patient and Doctor.

Abstraction is implemented using service interfaces such as
BillingCalculator.

Polymorphism is implemented through multiple PaymentProcessor
implementations such as cash, card, and mobile banking processors.

Docker Compose is used to simplify local application and PostgreSQL
setup.
```

---

# 69. Development Priority

Use this priority order:

```text
HIGH
Project Setup
Database
Patient
Doctor
Appointment
Billing
OOP Concepts

MEDIUM
Dashboard
Search
Validation
Responsive UI

LOW
Authentication
Charts
PDF
Email
Advanced reporting
```

---

# 70. Final Recommended Repository Milestones

Suggested commits:

```text
chore: initialize spring boot project

chore: add docker development environment

feat: add common person domain model

feat: implement patient management

feat: implement doctor management

feat: implement appointment management

feat: prevent doctor appointment conflicts

feat: implement billing management

feat: implement polymorphic payment processing

feat: add dashboard

style: improve responsive application UI

docs: add project documentation
```

---

# 71. Project Completion Definition

The project is complete when:

1. A fresh clone can be started with Docker Compose.
2. PostgreSQL data persists.
3. Patients can be managed.
4. Doctors can be managed.
5. Appointments can be managed.
6. Doctor schedule conflicts are prevented.
7. Bills can be created.
8. Payments can be recorded.
9. Payment status updates automatically.
10. Dashboard shows useful summaries.
11. UI is interactive and responsive.
12. Java OOP principles can be clearly demonstrated.
13. Project documentation explains setup and architecture.

---

# 72. Final Recommendation

For this assignment, avoid microservices.

Use:

```text
Spring Boot Modular Monolith
        +
PostgreSQL
        +
Thymeleaf
        +
Bootstrap
        +
Docker Compose
```

This architecture is:

- easier to develop
- easier to debug
- easier to demonstrate
- easier to dockerize
- suitable for a varsity assignment
- still capable of demonstrating professional architecture
- excellent for showing OOP concepts

The most important academic part is not the number of features.

The most important part is that the code clearly demonstrates:

```text
Encapsulation
Abstraction
Inheritance
Polymorphism
```

while solving a meaningful real-world problem.

---

# 73. Recommended Starting Prompt for Your Agent

Use this as your first agentic AI prompt:

```text
You are helping me develop a varsity Java OOP assignment project.

Project:
Hospital Management System

Stack:
- Java 21
- Spring Boot
- Spring MVC
- Thymeleaf
- Bootstrap
- Spring Data JPA
- PostgreSQL
- Maven
- Docker Compose

Core Modules:
- Patient Management
- Doctor Management
- Appointment Management
- Billing Management
- Dashboard

Academic Requirement:
The project must clearly demonstrate:
- Encapsulation
- Abstraction
- Inheritance
- Polymorphism

Architecture:
Use a simple layered modular-monolith architecture.

Important:
Do not generate the entire project immediately.

For now complete only Phase 1:

1. Initialize the Spring Boot project.
2. Configure Maven dependencies.
3. Create the recommended package structure.
4. Configure PostgreSQL properties using environment variables.
5. Create a simple home/dashboard controller.
6. Create a basic Thymeleaf + Bootstrap layout.
7. Add a health/startup page.
8. Do not implement domain modules yet.
9. Keep all code simple, readable, and suitable for explanation during
   a varsity viva.
10. After completing the phase, list changed files and explain how to
    run and verify the application manually.
```

After Phase 1 works, continue with one phase at a time from this document.
