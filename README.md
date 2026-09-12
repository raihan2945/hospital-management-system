# Hospital Management System

A Java 21 varsity assignment project following the [project blueprint](docs/hospital-management-system--blueprint.md).

Phases 1 through 5 provide a running Spring Boot application, persistent
PostgreSQL storage, the common OOP foundation, and complete patient and doctor
directories. Appointments, billing, related histories, and dashboard statistics
are scheduled for later phases.

## Stack and structure

- Java 21, Maven, Spring Boot 3.5.16
- Spring MVC, Thymeleaf, Bootstrap 5.3.8 (served locally from a WebJar)
- Spring Data JPA, Jakarta Bean Validation, PostgreSQL 17
- Spring Boot Actuator for application and database health
- Docker multi-stage build and Docker Compose

```text
src/main/java/com/example/hms/
  HospitalManagementApplication.java
  controller/             HomeController, PatientController, DoctorController
  config/                 Existing-record registration code initialization
  domain/                 BaseEntity, Person, Patient, Doctor
  domain/enums/           Gender
  dto/                    PersonForm, PatientForm, DoctorForm
  repository/             Patient/doctor repositories and directory search
  service/                PatientService, DoctorService and transactions
  service/payment/        Future payment implementations
  exception/              Application errors and handling
  util/                   Shared helpers
src/main/resources/
  application.yml
  templates/home.html
  templates/layout/fragments.html
  templates/layout/forms.html
  templates/patients/      List, form, and details pages
  templates/doctors/       List, form, and details pages
  templates/directory/    Delete confirmation
  templates/error/        Friendly missing-record and invalid-request pages
  static/css/app.css
src/test/                 Domain, persistence, startup, page, and health tests
```

The base packages are documented with `package-info.java`. Features follow controller → service → repository
→ PostgreSQL, with entities representing domain state and DTOs validating forms.
The home controller only selects a view. Shared Thymeleaf fragments provide the
page head, navigation, and footer.

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
the database volume. No demo patient or doctor records are added automatically.

Specialization suggestions allow custom values; the list filter uses the actual
stored specializations. Consultation fees accept zero through `9999999999.99`,
with at most two decimal places. Invalid dates, malformed fees, missing required
fields, and invalid email addresses are shown on the form while preserving input.
Missing records return a friendly HTTP 404, invalid URL/filter types return 400,
and database constraint conflicts return 409.

Deletion currently removes the record permanently after confirmation. Associated
appointment and billing history belongs to phases 6 and 7; deletion rules must be
extended when those relationships are introduced.

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

3. Open [the home page](http://localhost:8080). `/dashboard` serves the same
   starter page. Open [system health](http://localhost:8080/actuator/health) and
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
or extends the `patients` and `doctors` tables. Schema migrations can be
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
```

Both tables should contain `id`, `created_at`, `updated_at`, `first_name`,
`last_name`, `phone`, and `email`, alongside the subclass-specific fields.
The registration code columns have unique constraints. The tables start empty;
register records through the application to populate them.

On PowerShell, the HTTP checks are:

```powershell
(Invoke-WebRequest -UseBasicParsing http://localhost:8080).StatusCode
Invoke-RestMethod http://localhost:8080/actuator/health
```

Expect HTTP 200 and database status `UP`. Check the page at a narrow browser
width: navigation should collapse, cards should stack, and all styling and scripts
should load locally. Patient and doctor cards link to their directories; the
appointment and billing cards remain informational.

To check health failure and recovery in your local setup, stop PostgreSQL with
`docker compose stop postgres`. The health endpoint should return HTTP 503 with
database status `DOWN`. Restart it with `docker compose start postgres`; health
should recover to `UP` once a connection is available.

Framework references: [Spring Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html),
[Actuator health](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html),
and [Bootstrap documentation](https://getbootstrap.com/docs/5.3/getting-started/introduction/).
