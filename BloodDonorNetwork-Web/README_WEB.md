# Blood Donor Network: Clinical Web Platform

A clinical blood bank management web application built for college OOP and DBMS internals demonstration using **Java 25+** (compiled for Java 25), **Spring Boot 3.5.0**, **Spring MVC**, **Thymeleaf**, **HTML5/CSS3/JavaScript**, and **MySQL 8** with **JDBC**.

---

## 1. Architecture and Design Principles

- **Framework**: Spring Boot 3.5.0 with embedded Tomcat.
- **View Layer**: Server-rendered Thymeleaf templates with fragment reuse (`layout.html`).
- **Database**: MySQL 8 accessed via JDBC connection pooling (`HikariCP`), strictly reusing tested business logic and DAO patterns.
- **Clinical Integrity**:
  - **ACID Blood Issuance**: Managed via `SELECT ... FOR UPDATE` row locks in `RequestWorkflowService.java` to prevent race conditions during inventory fulfillment.
  - **Automated Stock Sync**: Database triggers (`after_donation_insert`) update stock and sync the donor recovery date atomically upon whole blood donation.
  - **Strict Donor Recovery**: Enforces 90-day recovery intervals and age boundaries (18 to 65 years).
  - **In-App Clinical Notifications**: Facilities can notify eligible and available donors; donors accept or decline in their portal.
  - **Zero External CDNs**: Offline capability with local vendor scripts (`lucide.min.js`, `chart.umd.min.js`) in `src/main/resources/static/vendor/`.
  - **Strict Design Standards**: Professional medical-red and slate theme, solid colors, layered soft shadows, 6px to 8px corner radii, zero emoji icons, zero em or en dashes in visible copy.

---

## 2. Database Migrations

All migrations are additive, idempotent, and located in `db/migrations/`:

1. `db/migrations/001_request_events.sql`
   - Creates `request_event` audit trail table tracking demand lifecycle state transitions (`CREATED`, `MATCHED`, `FULFILLED`, `CANCELLED`).
2. `db/migrations/002_donor_notifications.sql`
   - Creates `donor_notification` table with a unique composite key `(donor_id, request_id)` to prevent spam.
3. `db/migrations/003_donor_achievements.sql`
   - Creates `achievement_definition` and `donor_achievement` tables with 4 verified milestone awards (`FIRST_DROP`, `LIFE_SAVER`, `COMMUNITY_HERO`, `DEDICATED_DONOR`).

### Applying Migrations (if not already applied):
```bash
mysql -u root -p blood_network < db/migrations/001_request_events.sql
mysql -u root -p blood_network < db/migrations/002_donor_notifications.sql
mysql -u root -p blood_network < db/migrations/003_donor_achievements.sql
```

---

## 3. Demo Credentials

| Role | Username | Password | Linked Entity / Scope |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Central oversight, all hospitals, full stock management |
| **Hospital Staff** | `apollo_chennai` | `hospital123` | Apollo Speciality Hospital (Chennai) |
| **Hospital Staff** | `kmch_cbe` | `hospital123` | KMCH Hospital (Coimbatore) |
| **Voluntary Donor** | `rahul_sharma` | `donor123` | Rahul Sharma (A+, Eligible) |
| **Voluntary Donor** | `priya_ramesh` | `donor123` | Priya Ramesh (O+, In 90-day recovery) |

*Note: Passwords are transparently upgraded from legacy SHA-256 to BCrypt on valid login. Brute-force protection locks accounts for 10 minutes after 5 consecutive failed attempts.*

---

## 4. How to Run

### Option A: Using the Windows Launcher Script (Recommended)
From the root project directory:
```cmd
run-web.bat
```
`run-web.bat` automatically parses credentials from `db.properties` (or reads `DB_USER` and `DB_PASSWORD` from the environment) and starts the Spring Boot server.

### Option B: Using Maven Wrapper Directly
```cmd
cd BloodDonorNetwork-Web
.\mvnw.cmd spring-boot:run
```

Once started, open your browser at:
```
http://localhost:8080
```

---

## 5. Live Demonstration Script

1. **Public Landing Page (`/`)**:
   - Inspect clinical hero section with pauseable SVG heartbeat line.
   - Interactive Hemolytic Compatibility Matrix: Click patient blood groups (e.g. A+, O-, AB+) to see real-time compatibility highlights and clinical rules.
   - Clinical workflow overview (Demand -> Match -> Notify -> Transfusion).

2. **Hospital Workflow (`/login` as `hospital_apollo` / `hospital123`)**:
   - View facility-isolated inventory and active patient requests.
   - Click **Raise Blood Demand**: Create emergency demand (e.g. Patient: *Ravi Kumar*, Group: *A+*, Units: *2*, Urgency: *CRITICAL*).
   - In the Requests list, click **Match Donors**: View intelligent donor matching (Exact vs Compatible).
   - Click **Send Notification**: Sends an in-app clinical alert to eligible voluntary donors.
   - Click **View Details**: Inspect demand lifecycle, smart inventory allocation plan, and audit log.

3. **Donor Workflow (`/login` as `donor_rahul` / `donor123`)**:
   - View donor eligibility status card (90-day recovery timer and next eligible date).
   - Check **Clinical Alerts & Requests**: View pending blood alert from Apollo Hospital.
   - Click **Accept Alert**: Notification status updates to `ACCEPTED` and hospital demand status updates to `MATCHED`.
   - Add to Calendar: Click **Download Calendar Reminder (.ics)** to save next eligible donation date to Google/Apple Calendar.
   - View **Verified Milestones**: Badges awarded exclusively from real donation records.

4. **Administrator Console (`/login` as `admin` / `admin123`)**:
   - Central KPI metrics: Donors, hospitals, open demands, total inventory.
   - **Analytical Telemetry (Chart.js)**:
     - Grouped Bar Chart: Stock vs Open Demand per blood group (O-, O+, A-, A+, B-, B+, AB-, AB+).
     - Status breakdown doughnut chart and monthly donation trend line.
   - **Record Verified Donation**: Click **Record Donation**, select donor and receiving hospital. Submitting invokes the MySQL trigger, automatically crediting stock and updating donor recovery dates.
   - **ACID Blood Issuance**: Click on any pending request and click **Issue Blood**. Row-level lock deducts stock and fulfills the demand atomically.
   - **CSV Export**: Click **Requests CSV** or **Inventory CSV** for instant downloadable audit reports.

---

## 6. Running Tests

Unit tests verify compatibility rules, 90-day recovery calculations, and state machine lifecycle transitions:
```cmd
cd BloodDonorNetwork-Web
.\mvnw.cmd test
```

---

## 7. Desktop Swing Application Preservation

The original Swing desktop application and its batch scripts remain 100% untouched and functional:
- Compile Swing app: `build.bat`
- Run Swing app: `run.bat`
