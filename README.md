# Blood Donor Network

A college project built with **Java 25+** (compiled for Java 25), **MySQL 8.0+**, **JDBC**, and **Java Swing (GUI)** to connect voluntary blood donors with hospitals, match urgent blood requests, track hospital inventories with automated database triggers, issue blood using ACID transactions, and provide role-tailored desktop dashboards for Administrators, Donors, and Hospital Staff.

---

## Project Structure

```text
BloodDonorNetwork/
├── db/
│   ├── schema.sql                   # Database creation and 6 core tables
│   ├── sample_data.sql              # Fictional starter data (hospitals, donors, requests, stock)
│   ├── triggers.sql                 # Automated database triggers for stock & donor date updates
│   ├── demo_accounts.sql            # Phase 5A: SHA-256 password hashes for demo accounts
│   └── queries.sql                  # Verification queries (eligibility, pending requests, stock)
├── docs/                            # Project documentation
├── lib/
│   └── mysql-connector-j-8.3.0.jar  # MySQL JDBC driver for standard javac/java execution
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── bloodnetwork/
│       │           ├── Main.java                        # Console UI fallback with menu options (1-18)
│       │           ├── dao/
│       │           │   ├── DonationDAO.java             # Donation CRUD (JDBC)
│       │           │   ├── DonorDAO.java                # Donor CRUD & dynamic IN (?, ?) queries
│       │           │   ├── HospitalDAO.java             # Hospital CRUD (JDBC)
│       │           │   ├── ReportDAO.java               # Analytical reports with JOINs & GROUP BY
│       │           │   ├── RequestDAO.java              # Request management & hospital queries
│       │           │   ├── StockDAO.java                # Hospital & aggregate inventory tracking
│       │           │   └── UserDAO.java                 # User authentication lookups & entity links
│       │           ├── exception/
│       │           │   ├── AccessDeniedException.java   # Thrown on hospital/donor permission violations
│       │           │   ├── AuthenticationException.java # Thrown on invalid login credentials
│       │           │   ├── InsufficientStockException.java # Thrown when hospital stock < needed
│       │           │   ├── InvalidInputException.java   # Custom input validation exception
│       │           │   ├── NoMatchFoundException.java   # Thrown when donor matching finds 0 results
│       │           │   └── NotEligibleException.java    # Thrown when donor fails 90-day rule or age
│       │           ├── matching/
│       │           │   ├── BloodMatcher.java            # Matching strategy interface
│       │           │   ├── CompatibilityTable.java      # Medical transfusion compatibility table
│       │           │   ├── CompatibleMatcher.java       # Transfusion-compatible matching strategy
│       │           │   ├── DonorPriorityComparator.java # Prioritizes exact match, never donated, date
│       │           │   └── ExactMatcher.java            # Same-blood-group matching strategy
│       │           ├── model/
│       │           │   ├── Admin.java                   # Admin model (getDashboardMenu polymorphism)
│       │           │   ├── AppUser.java                 # Database user credentials entity
│       │           │   ├── BloodRequest.java            # Blood request entity
│       │           │   ├── BloodStock.java              # Blood stock inventory entity
│       │           │   ├── Donation.java                # Donation event entity
│       │           │   ├── Donor.java                   # Donor model with eligibility & menu logic
│       │           │   ├── Hospital.java                # Hospital model
│       │           │   ├── RequestStatus.java           # Enum: PENDING, MATCHED, FULFILLED, CANCELLED
│       │           │   ├── Requester.java               # Requester model with menu & hospital link
│       │           │   ├── Urgency.java                 # Enum: CRITICAL, HIGH, NORMAL
│       │           │   └── User.java                    # Abstract base model
│       │           ├── service/
│       │           │   ├── AuthService.java             # Secure login & session management
│       │           │   ├── BloodIssueService.java       # ACID transactional blood issuance
│       │           │   ├── DonationService.java         # Donation recording & eligibility validation
│       │           │   ├── RegistrationService.java     # Atomic donor self-registration transaction
│       │           │   └── RequestService.java          # Hospital authorization & request cancellation
│       │           ├── ui/
│       │           │   ├── AdminDashboard.java          # Tabbed administrative control panel
│       │           │   ├── App.java                     # Main entry point for Swing GUI
│       │           │   ├── DonationsPanel.java          # Donations JTable & recording dialog
│       │           │   ├── DonorDashboard.java          # Phase 5B: Dedicated donor portal
│       │           │   ├── DonorsPanel.java             # Donors JTable, search, add/edit/delete
│       │           │   ├── HospitalsPanel.java          # Hospitals JTable, add/edit/delete
│       │           │   ├── LoginFrame.java              # Sign-in window with Sign-Up link
│       │           │   ├── PlaceholderFrame.java        # Fallback placeholder window
│       │           │   ├── ReportsPanel.java            # Interactive analytical reporting table
│       │           │   ├── RequesterDashboard.java      # Phase 5B: Hospital requester portal
│       │           │   ├── RequestsPanel.java           # Requests JTable, matching, issue & cancel
│       │           │   ├── SignUpDialog.java            # Phase 5B: Donor registration modal
│       │           │   ├── StockPanel.java              # Inventory table with low stock highlights
│       │           │   └── UITheme.java                 # Medical red branding theme & styles
│       │           └── util/
│       │               ├── DBConnection.java            # Connection manager with safe pooling
│       │               ├── PasswordUtil.java            # SHA-256 hashing & verification
│       │               ├── Session.java                 # Global logged-in user session
│       │               └── Validator.java               # Static validation utility
│       └── resources/
│           ├── db.properties.example                    # Template configuration file
│           └── db.properties                            # Local configuration (git-ignored)
├── build.bat                        # One-click Windows batch script to compile without Maven
├── run.bat                          # One-click Windows batch script to launch the console app
├── run_gui.bat                      # One-click Windows batch script to launch the Swing GUI
├── pom.xml                          # Maven build file (Java 25 & MySQL Connector/J)
├── .gitignore                       # Ignores credentials and compiled binaries
└── README.md                        # Project overview and setup instructions
```

---

## Database Setup Reference

Always execute the database scripts in this order:

1. `db/schema.sql` (Creates the `blood_network` database and 6 core tables)
2. `db/sample_data.sql` (Inserts initial sample records)
3. `db/triggers.sql` (**Phase 4**: Automates inventory and donor date updates via triggers)
4. `db/demo_accounts.sql` (**Phase 5A**: Sets known SHA-256 demo passwords for GUI login)
5. `db/queries.sql` (Runs verification test queries)

### Running Database Scripts via Command Line (Terminal / CMD)
```bash
mysql -u root -p blood_network < db/schema.sql
mysql -u root -p blood_network < db/sample_data.sql
mysql -u root -p blood_network < db/triggers.sql
mysql -u root -p blood_network < db/demo_accounts.sql
mysql -u root -p blood_network < db/queries.sql
```

### Running Database Scripts via MySQL Workbench
1. Open MySQL Workbench and connect to your local MySQL instance.
2. Open and execute `db/schema.sql` (click ⚡).
3. Open and execute `db/sample_data.sql` (click ⚡).
4. Open and execute `db/triggers.sql` (click ⚡).
5. Open and execute `db/demo_accounts.sql` (click ⚡) to activate demo accounts.
6. Open and execute `db/queries.sql` (click ⚡) to verify.

---

## Demo Login Accounts

| Role | Username | Password | Access Description |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `admin123` | Full access to Admin Dashboard (Donors, Hospitals, Requests, Donations, Stock, Reports) |
| **REQUESTER** | `apollo_chennai` | `hospital123` | Hospital Requester portal for Apollo Hospitals (Chennai) |
| **REQUESTER** | `kmch_cbe` | `hospital123` | Hospital Requester portal for KMCH (Coimbatore) |
| **REQUESTER** | `meenakshi_mdu` | `hospital123` | Hospital Requester portal for Meenakshi Mission (Madurai) |
| **DONOR** | `rahul_sharma` | `donor123` | Donor portal (Chennai, O+, eligible) |
| **DONOR** | `priya_ramesh` | `donor123` | Donor portal (Coimbatore, A-, eligible) |
| **DONOR** | `anand_kumar` | `donor123` | Donor portal (Madurai, B+, eligible) |

---

## How to Compile and Run (No Maven Required - Standard Java)

### Step 1: Compile the Project
From the project folder (`BloodDonorNetwork`):
- Double-click `build.bat` (or type `build.bat` in Command Prompt):
  ```cmd
  build.bat
  ```

---

### Step 2: Run the Application

#### Option A: Launch the Swing Desktop GUI (Recommended)
- Double-click `run_gui.bat` (or type `run_gui.bat` in Command Prompt):
  ```cmd
  run_gui.bat
  ```
- Or run manually via CMD:
  ```cmd
  java -cp "bin;lib/*;src/main/resources" com.bloodnetwork.ui.App
  ```

#### Option B: Launch the Terminal Console Menu (Fallback)
- Double-click `run.bat` (or type `run.bat` in Command Prompt):
  ```cmd
  run.bat
  ```
- Or run manually via CMD:
  ```cmd
  java -cp "bin;lib/*;src/main/resources" com.bloodnetwork.Main
  ```

---

## GUI Dashboards & Role Portals (Phase 5A & 5B)

### 1. Donor Portal (`ui.DonorDashboard`)
When a donor logs in, the portal displays a personalized header and tabs dynamically generated from `donor.getDashboardMenu()`:
- **My Profile Tab**: View personal details and edit Name, Age, Phone, City, and availability status. Blood group is kept read-only for medical integrity. All modifications are validated through `Validator`.
- **Eligibility Tab**: Features a visual status card driven by `donor.isEligible(90)`:
  - **Eligible (Green)**: Indicates readiness to donate immediately, displaying the last donation date.
  - **Not Eligible (Red)**: Explains the exact clinical reason (underage, outside age window, marked unavailable, or the remaining countdown days and next eligible date).
- **Donation History Tab**: `JTable` detailing all past donations made by this donor (Date, Hospital Name, Units Contributed) with an aggregate total counter.
- **Open Requests Tab**: Displays urgent/pending blood demands posted by hospitals in the donor's city that match their blood compatibility via `CompatibilityTable.donorGroupsFor(...)`.

### 2. Hospital Requester Portal (`ui.RequesterDashboard`)
When a hospital staff member logs in, the portal strictly scopes all data and operations to their assigned institution:
- **Raise Request Tab**: Intuitive form to submit patient blood requests with urgency levels (`CRITICAL`, `HIGH`, `NORMAL`). The requesting hospital ID is automatically bound from the active session.
- **My Requests Tab**: `JTable` displaying only requests originating from this hospital. Staff can filter by status and cancel their own requests. Security is enforced by `RequestService.cancelOwnRequest(...)`, raising `AccessDeniedException` if another hospital's request is targeted.
- **Find Donors Tab**: Allows selecting an active request and searching for eligible donors in the hospital's city using **Exact Match** or **Compatible Match** strategies. When matches are found, the request status is automatically updated to `MATCHED`.
- **Our Stock Tab**: Real-time inventory table of the hospital's blood units by group. Automatically highlights critically low inventory (< 3 units) in soft red (`#ffcdd2`).

### 3. Self-Service Donor Registration (`ui.SignUpDialog`)
New voluntary blood donors can sign up directly from the login window via the **"New donor? Sign up"** button:
- Collects Username, Password (minimum 6 characters), Full Name, Age (18–65), Gender, Blood Group, 10-digit Phone, and City.
- Implements an atomic ACID transaction in `RegistrationService.registerDonor(...)` that hashes the password with SHA-256, inserts the user into `app_user`, and registers the profile in `donor` within a single commit.

### 4. Administrator Dashboard (`ui.AdminDashboard`)
Full system management interface covering Donors, Hospitals, Global Requests, Donations, Stock Management, and Analytical Reports (`ReportDAO`).
