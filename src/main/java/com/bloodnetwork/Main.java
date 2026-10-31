package com.bloodnetwork;

import com.bloodnetwork.dao.DonationDAO;
import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.ReportDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.dao.StockDAO;
import com.bloodnetwork.exception.InsufficientStockException;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.exception.NoMatchFoundException;
import com.bloodnetwork.exception.NotEligibleException;
import com.bloodnetwork.matching.BloodMatcher;
import com.bloodnetwork.matching.CompatibleMatcher;
import com.bloodnetwork.matching.ExactMatcher;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.BloodStock;
import com.bloodnetwork.model.Donation;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.model.Urgency;
import com.bloodnetwork.service.BloodIssueService;
import com.bloodnetwork.service.DonationService;
import com.bloodnetwork.util.Validator;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Main application console interface:
 * Provides menu-driven management for Donors, Hospitals, Blood Requests,
 * Transfusion Compatibility Matching, Blood Inventory, Donations,
 * Transactional Blood Issuance, and Analytical Reports.
 */
public class Main {

    private static final DonorDAO donorDAO = new DonorDAO();
    private static final HospitalDAO hospitalDAO = new HospitalDAO();
    private static final RequestDAO requestDAO = new RequestDAO();
    private static final StockDAO stockDAO = new StockDAO();
    private static final DonationDAO donationDAO = new DonationDAO();
    private static final ReportDAO reportDAO = new ReportDAO();

    private static final DonationService donationService = new DonationService(donationDAO, donorDAO, hospitalDAO, requestDAO);
    private static final BloodIssueService bloodIssueService = new BloodIssueService(requestDAO, stockDAO, hospitalDAO);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=================================================");
        System.out.println("       BLOOD DONOR NETWORK MANAGEMENT SYSTEM     ");
        System.out.println("=================================================");

        while (running && scanner.hasNextLine()) {
            printMenu();
            System.out.print("Enter your choice (0-18): ");
            String input = scanner.nextLine().trim();

            try {
                switch (input) {
                    case "1":
                        handleAddDonor(scanner);
                        break;
                    case "2":
                        handleViewAllDonors();
                        break;
                    case "3":
                        handleUpdateDonor(scanner);
                        break;
                    case "4":
                        handleDeleteDonor(scanner);
                        break;
                    case "5":
                        handleSearchDonorsByCity(scanner);
                        break;
                    case "6":
                        handleCheckDonorEligibility(scanner);
                        break;
                    case "7":
                        handleAddHospital(scanner);
                        break;
                    case "8":
                        handleViewAllHospitals();
                        break;
                    case "9":
                        handleRaiseBloodRequest(scanner);
                        break;
                    case "10":
                        handleViewPendingRequests();
                        break;
                    case "11":
                        handleFindMatchingDonors(scanner);
                        break;
                    case "12":
                        handleUpdateOrCancelRequest(scanner);
                        break;
                    case "13":
                        handleViewBloodStock(scanner);
                        break;
                    case "14":
                        handleRecordDonation(scanner);
                        break;
                    case "15":
                        handleViewDonations(scanner);
                        break;
                    case "16":
                        handleDeleteDonation(scanner);
                        break;
                    case "17":
                        handleIssueBlood(scanner);
                        break;
                    case "18":
                        handleReportsMenu(scanner);
                        break;
                    case "0":
                        running = false;
                        System.out.println("\nThank you for using Blood Donor Network. Goodbye!");
                        break;
                    default:
                        System.out.println("[!] Invalid choice. Please enter a number between 0 and 18.");
                }
            } catch (InvalidInputException e) {
                System.out.println("[Input Error] " + e.getMessage());
            } catch (NotEligibleException e) {
                System.out.println("[Ineligibility Notice] " + e.getMessage());
            } catch (InsufficientStockException e) {
                System.out.println("[Insufficient Stock] " + e.getMessage());
            } catch (NoMatchFoundException e) {
                System.out.println("[No Match Found] " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("[Database Error] " + e.getMessage());
                System.out.println("--> Please ensure MySQL server is running and db.properties settings are correct.");
            } catch (Exception e) {
                System.out.println("[Unexpected Error] " + e.getMessage());
            }

            System.out.println(); // Blank line for cleaner console separation
        }

        scanner.close();
    }

    /**
     * Displays the interactive console navigation menu.
     */
    private static void printMenu() {
        System.out.println("-------------------------------------------------");
        System.out.println("--- DONOR & HOSPITAL MANAGEMENT ---");
        System.out.println(" 1. Add donor");
        System.out.println(" 2. View all donors");
        System.out.println(" 3. Update donor");
        System.out.println(" 4. Delete donor");
        System.out.println(" 5. Search donors by city");
        System.out.println(" 6. Check donor eligibility (90 days rule)");
        System.out.println(" 7. Add hospital");
        System.out.println(" 8. View all hospitals");
        System.out.println("--- BLOOD REQUESTS & MATCHING ---");
        System.out.println(" 9. Raise blood request");
        System.out.println(" 10. View pending requests (sorted by urgency)");
        System.out.println(" 11. Find matching donors for a request");
        System.out.println(" 12. Update or cancel a request");
        System.out.println(" 13. View blood stock by hospital and total stock");
        System.out.println("--- DONATIONS, TRANSACTIONS & REPORTS (PHASE 4) ---");
        System.out.println(" 14. Record donation");
        System.out.println(" 15. View donation history (by donor, and all)");
        System.out.println(" 16. Delete a donation (triggers reverse stock)");
        System.out.println(" 17. Issue blood for a request (ACID transaction)");
        System.out.println(" 18. Reports menu (analytical charts & stats)");
        System.out.println(" 0. Exit");
        System.out.println("-------------------------------------------------");
    }

    // ------------------------------------------------------------------------
    // Donor Operations (Options 1 - 6)
    // ------------------------------------------------------------------------

    private static void handleAddDonor(Scanner scanner) throws InvalidInputException, SQLException {
        System.out.println("\n--- [1] Add New Donor ---");

        System.out.print("Enter full name: ");
        String name = scanner.nextLine().trim();
        Validator.validateNonEmpty(name, "Donor Name");

        System.out.print("Enter age (18-65): ");
        int age = readInt(scanner, "Age");
        Validator.validateAge(age);

        System.out.print("Enter gender (M / F / O): ");
        String gender = scanner.nextLine().trim().toUpperCase();
        if (!gender.equals("M") && !gender.equals("F") && !gender.equals("O")) {
            throw new InvalidInputException("Gender must be 'M', 'F', or 'O'.");
        }

        System.out.print("Enter blood group (A+, A-, B+, B-, AB+, AB-, O+, O-): ");
        String bloodGroup = scanner.nextLine().trim().toUpperCase();
        Validator.validateBloodGroup(bloodGroup);

        System.out.print("Enter 10-digit phone number: ");
        String phone = scanner.nextLine().trim();
        Validator.validatePhone(phone);

        System.out.print("Enter city: ");
        String city = scanner.nextLine().trim();
        Validator.validateNonEmpty(city, "City");

        System.out.print("Enter last donation date (YYYY-MM-DD) or press Enter if never donated: ");
        String dateStr = scanner.nextLine().trim();
        LocalDate lastDonationDate = parseDateOrNull(dateStr);

        System.out.print("Is donor currently available to donate? (Y/N, default Y): ");
        String availStr = scanner.nextLine().trim().toUpperCase();
        boolean available = !availStr.equals("N");

        Donor donor = new Donor(name, age, gender, bloodGroup, phone, city, lastDonationDate, available);
        int generatedId = donorDAO.add(donor);

        System.out.println("[SUCCESS] Donor added successfully with Donor ID: " + generatedId);
    }

    private static void handleViewAllDonors() throws SQLException {
        System.out.println("\n--- [2] All Registered Donors ---");
        List<Donor> donors = donorDAO.getAll();
        if (donors.isEmpty()) {
            System.out.println("No donor records found in the database.");
            return;
        }

        System.out.printf("%-5s | %-16s | %-4s | %-3s | %-6s | %-12s | %-12s | %-12s | %-9s%n",
                "ID", "Name", "Grp", "Age", "Gender", "Phone", "City", "Last Donated", "Available");
        System.out.println("--------------------------------------------------------------------------------------------------");

        for (Donor d : donors) {
            System.out.printf("%-5d | %-16s | %-4s | %-3d | %-6s | %-12s | %-12s | %-12s | %-9s%n",
                    d.getDonorId(),
                    truncate(d.getName(), 16),
                    d.getBloodGroup(),
                    d.getAge(),
                    (d.getGender() != null ? d.getGender() : "-"),
                    d.getPhone(),
                    truncate(d.getCity(), 12),
                    (d.getLastDonationDate() != null ? d.getLastDonationDate().toString() : "Never"),
                    (d.isAvailable() ? "YES" : "NO"));
        }
        System.out.println("Total donors: " + donors.size());
    }

    private static void handleUpdateDonor(Scanner scanner) throws InvalidInputException, SQLException {
        System.out.println("\n--- [3] Update Donor Details ---");
        System.out.print("Enter Donor ID to update: ");
        int donorId = readInt(scanner, "Donor ID");

        Donor donor = donorDAO.getById(donorId);
        if (donor == null) {
            System.out.println("[!] Donor with ID " + donorId + " was not found.");
            return;
        }

        System.out.println("Found: " + donor.getName() + " (" + donor.getBloodGroup() + ", " + donor.getCity() + ")");
        System.out.println("(Press Enter on any field to keep its existing value)\n");

        System.out.print("Name [" + donor.getName() + "]: ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) {
            donor.setName(name);
        }

        System.out.print("Age [" + donor.getAge() + "]: ");
        String ageStr = scanner.nextLine().trim();
        if (!ageStr.isEmpty()) {
            try {
                int age = Integer.parseInt(ageStr);
                Validator.validateAge(age);
                donor.setAge(age);
            } catch (NumberFormatException e) {
                throw new InvalidInputException("Age must be a valid number.");
            }
        }

        System.out.print("Gender (M/F/O) [" + donor.getGender() + "]: ");
        String gender = scanner.nextLine().trim().toUpperCase();
        if (!gender.isEmpty()) {
            if (!gender.equals("M") && !gender.equals("F") && !gender.equals("O")) {
                throw new InvalidInputException("Gender must be 'M', 'F', or 'O'.");
            }
            donor.setGender(gender);
        }

        System.out.print("Blood Group [" + donor.getBloodGroup() + "]: ");
        String bg = scanner.nextLine().trim().toUpperCase();
        if (!bg.isEmpty()) {
            Validator.validateBloodGroup(bg);
            donor.setBloodGroup(bg);
        }

        System.out.print("Phone [" + donor.getPhone() + "]: ");
        String phone = scanner.nextLine().trim();
        if (!phone.isEmpty()) {
            Validator.validatePhone(phone);
            donor.setPhone(phone);
        }

        System.out.print("City [" + donor.getCity() + "]: ");
        String city = scanner.nextLine().trim();
        if (!city.isEmpty()) {
            donor.setCity(city);
        }

        String curDate = donor.getLastDonationDate() != null ? donor.getLastDonationDate().toString() : "None";
        System.out.print("Last Donation Date (YYYY-MM-DD or 'none') [" + curDate + "]: ");
        String dateStr = scanner.nextLine().trim();
        if (!dateStr.isEmpty()) {
            if (dateStr.equalsIgnoreCase("none")) {
                donor.setLastDonationDate(null);
            } else {
                donor.setLastDonationDate(parseDateOrNull(dateStr));
            }
        }

        System.out.print("Available? (Y/N) [" + (donor.isAvailable() ? "Y" : "N") + "]: ");
        String availStr = scanner.nextLine().trim().toUpperCase();
        if (!availStr.isEmpty()) {
            donor.setAvailable(availStr.equals("Y"));
        }

        boolean updated = donorDAO.update(donor);
        if (updated) {
            System.out.println("[SUCCESS] Donor ID " + donorId + " updated successfully.");
        } else {
            System.out.println("[!] Failed to update donor.");
        }
    }

    private static void handleDeleteDonor(Scanner scanner) throws InvalidInputException, SQLException {
        System.out.println("\n--- [4] Delete Donor ---");
        System.out.print("Enter Donor ID to delete: ");
        int donorId = readInt(scanner, "Donor ID");

        Donor donor = donorDAO.getById(donorId);
        if (donor == null) {
            System.out.println("[!] Donor with ID " + donorId + " was not found.");
            return;
        }

        System.out.print("Are you sure you want to delete donor '" + donor.getName() + "' (ID: " + donorId + ")? (Y/N): ");
        String confirm = scanner.nextLine().trim().toUpperCase();

        if (confirm.equals("Y")) {
            boolean deleted = donorDAO.delete(donorId);
            if (deleted) {
                System.out.println("[SUCCESS] Donor ID " + donorId + " deleted successfully.");
            } else {
                System.out.println("[!] Delete operation failed.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private static void handleSearchDonorsByCity(Scanner scanner) throws InvalidInputException, SQLException {
        System.out.println("\n--- [5] Search Donors by City ---");
        System.out.print("Enter city name: ");
        String city = scanner.nextLine().trim();
        Validator.validateNonEmpty(city, "City");

        List<Donor> results = donorDAO.findByCity(city);
        if (results.isEmpty()) {
            System.out.println("No donors found in city: '" + city + "'.");
            return;
        }

        System.out.println("\nFound " + results.size() + " donor(s) in " + city + ":");
        for (Donor d : results) {
            System.out.println(d);
        }
    }

    private static void handleCheckDonorEligibility(Scanner scanner) throws InvalidInputException, SQLException {
        System.out.println("\n--- [6] Check Donor Eligibility (90 Days Medical Rule) ---");
        System.out.print("Enter Donor ID to verify: ");
        int donorId = readInt(scanner, "Donor ID");

        Donor donor = donorDAO.getById(donorId);
        if (donor == null) {
            System.out.println("[!] Donor with ID " + donorId + " was not found.");
            return;
        }

        int gapDays = 90;
        boolean eligible = donor.isEligible(gapDays);

        System.out.println("\n--- Eligibility Report for Donor #" + donor.getDonorId() + " (" + donor.getName() + ") ---");
        System.out.println("Blood Group      : " + donor.getBloodGroup());
        System.out.println("Age              : " + donor.getAge() + " (Requirement: 18 - 65)");
        System.out.println("Available Status : " + (donor.isAvailable() ? "Active (Available)" : "Inactive / Unavailable"));

        if (donor.getLastDonationDate() == null) {
            System.out.println("Last Donation    : Never donated before (First-time donor)");
        } else {
            long daysAgo = ChronoUnit.DAYS.between(donor.getLastDonationDate(), LocalDate.now());
            System.out.println("Last Donation    : " + donor.getLastDonationDate() + " (" + daysAgo + " days ago; Required: " + gapDays + " days)");
        }

        System.out.println("-------------------------------------------------");
        if (eligible) {
            System.out.println(">> RESULT: [ELIGIBLE TO DONATE] <<");
        } else {
            System.out.println(">> RESULT: [NOT ELIGIBLE TO DONATE TODAY] <<");
            if (donor.getAge() < 18 || donor.getAge() > 65) {
                System.out.println("   Reason: Age " + donor.getAge() + " is outside permitted range (18-65).");
            }
            if (!donor.isAvailable()) {
                System.out.println("   Reason: Donor has marked their status as unavailable.");
            }
            if (donor.getLastDonationDate() != null) {
                long daysAgo = ChronoUnit.DAYS.between(donor.getLastDonationDate(), LocalDate.now());
                if (daysAgo < gapDays) {
                    System.out.println("   Reason: Less than 90 days have passed since last donation (" + daysAgo + " / " + gapDays + " days).");
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Hospital Operations (Options 7 - 8)
    // ------------------------------------------------------------------------

    private static void handleAddHospital(Scanner scanner) throws InvalidInputException, SQLException {
        System.out.println("\n--- [7] Add New Hospital ---");

        System.out.print("Enter hospital name: ");
        String name = scanner.nextLine().trim();
        Validator.validateNonEmpty(name, "Hospital Name");

        System.out.print("Enter hospital city: ");
        String city = scanner.nextLine().trim();
        Validator.validateNonEmpty(city, "Hospital City");

        System.out.print("Enter hospital contact phone (optional, press Enter to skip): ");
        String phone = scanner.nextLine().trim();
        if (phone.isEmpty()) {
            phone = null;
        }

        Hospital hospital = new Hospital(name, city, phone);
        int generatedId = hospitalDAO.add(hospital);

        System.out.println("[SUCCESS] Hospital added successfully with Hospital ID: " + generatedId);
    }

    private static void handleViewAllHospitals() throws SQLException {
        System.out.println("\n--- [8] All Registered Hospitals ---");
        List<Hospital> hospitals = hospitalDAO.getAll();
        if (hospitals.isEmpty()) {
            System.out.println("No hospital records found in the database.");
            return;
        }

        System.out.printf("%-5s | %-35s | %-15s | %-15s%n", "ID", "Hospital Name", "City", "Phone");
        System.out.println("------------------------------------------------------------------------------");
        for (Hospital h : hospitals) {
            System.out.printf("%-5d | %-35s | %-15s | %-15s%n",
                    h.getHospitalId(),
                    truncate(h.getName(), 35),
                    truncate(h.getCity(), 15),
                    (h.getPhone() != null ? h.getPhone() : "N/A"));
        }
        System.out.println("Total hospitals: " + hospitals.size());
    }

    // ------------------------------------------------------------------------
    // Blood Request & Matching Operations (Options 9 - 13)
    // ------------------------------------------------------------------------

    private static void handleRaiseBloodRequest(Scanner scanner) throws InvalidInputException, SQLException {
        System.out.println("\n--- [9] Raise New Blood Request ---");

        System.out.print("Enter Hospital ID: ");
        int hospitalId = readInt(scanner, "Hospital ID");
        Hospital hospital = hospitalDAO.getById(hospitalId);
        if (hospital == null) {
            throw new InvalidInputException("Hospital with ID " + hospitalId + " does not exist.");
        }
        System.out.println("Hospital Selected: " + hospital.getName() + " (" + hospital.getCity() + ")");

        System.out.print("Enter patient name: ");
        String patientName = scanner.nextLine().trim();
        Validator.validateNonEmpty(patientName, "Patient Name");

        System.out.print("Enter blood group needed (A+, A-, B+, B-, AB+, AB-, O+, O-): ");
        String bloodGroup = scanner.nextLine().trim().toUpperCase();
        Validator.validateBloodGroup(bloodGroup);

        System.out.print("Enter units needed (positive integer): ");
        int unitsNeeded = readInt(scanner, "Units Needed");
        if (unitsNeeded <= 0) {
            throw new InvalidInputException("Units needed must be greater than 0.");
        }

        System.out.print("Enter urgency level (1. CRITICAL, 2. HIGH, 3. NORMAL, default 3): ");
        String urgStr = scanner.nextLine().trim().toUpperCase();
        Urgency urgency;
        if (urgStr.equals("1") || urgStr.equals("CRITICAL")) {
            urgency = Urgency.CRITICAL;
        } else if (urgStr.equals("2") || urgStr.equals("HIGH")) {
            urgency = Urgency.HIGH;
        } else {
            urgency = Urgency.NORMAL;
        }

        BloodRequest request = new BloodRequest(hospitalId, patientName, bloodGroup, unitsNeeded, urgency);
        int generatedId = requestDAO.add(request);

        System.out.println("[SUCCESS] Blood request created successfully!");
        System.out.println("  Request ID  : " + generatedId);
        System.out.println("  Hospital    : " + hospital.getName() + " (" + hospital.getCity() + ")");
        System.out.println("  Patient     : " + patientName);
        System.out.println("  Blood Group : " + bloodGroup);
        System.out.println("  Units Needed: " + unitsNeeded);
        System.out.println("  Urgency     : " + urgency);
        System.out.println("  Status      : PENDING");
    }

    private static void handleViewPendingRequests() throws SQLException {
        System.out.println("\n--- [10] Pending Blood Requests (Prioritized by Urgency) ---");
        List<BloodRequest> pendingList = requestDAO.getPendingByUrgency();
        if (pendingList.isEmpty()) {
            System.out.println("There are no pending blood requests at this time.");
            return;
        }

        System.out.printf("%-5s | %-4s | %-16s | %-18s | %-5s | %-5s | %-10s | %-19s%n",
                "ReqID", "Hosp", "Patient", "Hospital City", "Group", "Units", "Urgency", "Request Time");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        for (BloodRequest req : pendingList) {
            Hospital h = hospitalDAO.getById(req.getHospitalId());
            String city = (h != null) ? h.getCity() : "Unknown";

            System.out.printf("%-5d | %-4d | %-16s | %-18s | %-5s | %-5d | %-10s | %-19s%n",
                    req.getRequestId(),
                    req.getHospitalId(),
                    truncate(req.getPatientName(), 16),
                    truncate(city, 18),
                    req.getBloodGroup(),
                    req.getUnitsNeeded(),
                    req.getUrgency(),
                    (req.getRequestDate() != null ? req.getRequestDate().toString().replace('T', ' ') : "N/A"));
        }
        System.out.println("Total pending requests: " + pendingList.size());
    }

    private static void handleFindMatchingDonors(Scanner scanner) throws InvalidInputException, NoMatchFoundException, SQLException {
        System.out.println("\n--- [11] Find Matching Donors for Blood Request ---");
        System.out.print("Enter Blood Request ID: ");
        int requestId = readInt(scanner, "Request ID");

        BloodRequest request = requestDAO.getById(requestId);
        if (request == null) {
            throw new InvalidInputException("Blood Request with ID " + requestId + " was not found.");
        }

        Hospital hospital = hospitalDAO.getById(request.getHospitalId());
        String hospitalName = (hospital != null) ? hospital.getName() : ("Hospital #" + request.getHospitalId());
        String city = (hospital != null) ? hospital.getCity() : "Unknown";

        System.out.println("Request Summary:");
        System.out.println("  Patient: " + request.getPatientName() + " | Blood Group: " + request.getBloodGroup()
                + " | Units: " + request.getUnitsNeeded() + " | Urgency: " + request.getUrgency());
        System.out.println("  Hospital: " + hospitalName + " (City: " + city + ") | Current Status: " + request.getStatus());

        System.out.println("\nChoose Matching Mode:");
        System.out.println(" 1. Exact Match Only (same blood group: " + request.getBloodGroup() + ")");
        System.out.println(" 2. Compatible Match (transfusion compatible groups)");
        System.out.print("Enter choice (1 or 2, default 2): ");
        String matchChoice = scanner.nextLine().trim();

        BloodMatcher matcher;
        String modeName;
        if (matchChoice.equals("1")) {
            matcher = new ExactMatcher(donorDAO, hospitalDAO);
            modeName = "Exact Match";
        } else {
            matcher = new CompatibleMatcher(donorDAO, hospitalDAO);
            modeName = "Compatible Match";
        }

        System.out.println("\nSearching for eligible donors in " + city + " using " + modeName + "...");
        List<Donor> matches = matcher.findMatches(request);

        if (matches == null || matches.isEmpty()) {
            throw new NoMatchFoundException("No eligible donors found in " + city + " for Blood Request #" + requestId
                    + " (Blood Group: " + request.getBloodGroup() + ", Mode: " + modeName + "). "
                    + "All registered donors are either incompatible, inactive, or donated within the last 90 days.");
        }

        System.out.println("\n=== FOUND " + matches.size() + " MATCHING & ELIGIBLE DONOR(S) ===");
        System.out.printf("%-5s | %-18s | %-5s | %-14s | %-12s | %-14s%n",
                "ID", "Name", "Group", "City", "Phone", "Last Donation");
        System.out.println("----------------------------------------------------------------------------------");

        for (Donor d : matches) {
            String lastDon = (d.getLastDonationDate() != null) ? d.getLastDonationDate().toString() : "Never Donated";
            System.out.printf("%-5d | %-18s | %-5s | %-14s | %-12s | %-14s%n",
                    d.getDonorId(),
                    truncate(d.getName(), 18),
                    d.getBloodGroup(),
                    truncate(d.getCity(), 14),
                    d.getPhone(),
                    lastDon);
        }

        if (request.getStatus() == RequestStatus.PENDING) {
            boolean updated = requestDAO.updateStatus(requestId, RequestStatus.MATCHED);
            if (updated) {
                System.out.println("\n[SUCCESS] Request #" + requestId + " status updated from 'PENDING' to 'MATCHED'.");
            }
        } else {
            System.out.println("\n[INFO] Request #" + requestId + " is currently in status: " + request.getStatus());
        }
    }

    private static void handleUpdateOrCancelRequest(Scanner scanner) throws InvalidInputException, SQLException {
        System.out.println("\n--- [12] Update or Cancel Blood Request ---");
        System.out.print("Enter Blood Request ID: ");
        int requestId = readInt(scanner, "Request ID");

        BloodRequest req = requestDAO.getById(requestId);
        if (req == null) {
            System.out.println("[!] Blood Request with ID " + requestId + " was not found.");
            return;
        }

        System.out.println("Current Request: Patient: " + req.getPatientName() + " | Blood Group: " + req.getBloodGroup()
                + " | Units Needed: " + req.getUnitsNeeded() + " | Status: " + req.getStatus());

        System.out.println("Select Action:");
        System.out.println(" 1. Update units needed");
        System.out.println(" 2. Update status (PENDING / MATCHED / FULFILLED / CANCELLED)");
        System.out.println(" 3. Cancel request");
        System.out.println(" 0. Go back");
        System.out.print("Enter choice (0-3): ");
        String act = scanner.nextLine().trim();

        switch (act) {
            case "1":
                System.out.print("Enter new units needed (> 0): ");
                int newUnits = readInt(scanner, "New Units Needed");
                if (newUnits <= 0) {
                    throw new InvalidInputException("Units needed must be greater than 0.");
                }
                boolean uOk = requestDAO.updateUnits(requestId, newUnits);
                if (uOk) {
                    System.out.println("[SUCCESS] Units updated to " + newUnits + " for Request #" + requestId);
                } else {
                    System.out.println("[!] Failed to update units.");
                }
                break;

            case "2":
                System.out.print("Enter new status (PENDING / MATCHED / FULFILLED / CANCELLED): ");
                String stStr = scanner.nextLine().trim().toUpperCase();
                RequestStatus newStatus;
                try {
                    newStatus = RequestStatus.valueOf(stStr);
                } catch (IllegalArgumentException e) {
                    throw new InvalidInputException("Invalid status '" + stStr + "'. Allowed: PENDING, MATCHED, FULFILLED, CANCELLED.");
                }
                boolean sOk = requestDAO.updateStatus(requestId, newStatus);
                if (sOk) {
                    System.out.println("[SUCCESS] Request #" + requestId + " status changed to: " + newStatus);
                } else {
                    System.out.println("[!] Failed to update status.");
                }
                break;

            case "3":
                System.out.print("Are you sure you want to cancel Request #" + requestId + "? (Y/N): ");
                String c = scanner.nextLine().trim().toUpperCase();
                if (c.equals("Y")) {
                    boolean cOk = requestDAO.cancel(requestId);
                    if (cOk) {
                        System.out.println("[SUCCESS] Blood Request #" + requestId + " has been CANCELLED.");
                    } else {
                        System.out.println("[!] Failed to cancel request.");
                    }
                } else {
                    System.out.println("Action aborted.");
                }
                break;

            case "0":
                System.out.println("Returning to main menu.");
                break;

            default:
                System.out.println("[!] Invalid choice.");
        }
    }

    private static void handleViewBloodStock(Scanner scanner) throws SQLException {
        System.out.println("\n--- [13] Blood Stock Inventory ---");

        System.out.println("=== Total Aggregate Stock Across All Hospitals ===");
        Map<String, Integer> totals = stockDAO.getTotalByBloodGroup();
        if (totals.isEmpty()) {
            System.out.println("No blood stock records found.");
        } else {
            System.out.printf("%-12s | %-15s%n", "Blood Group", "Total Available");
            System.out.println("-----------------------------");
            int grandTotal = 0;
            for (Map.Entry<String, Integer> entry : totals.entrySet()) {
                System.out.printf("%-12s | %-15d%n", entry.getKey(), entry.getValue());
                grandTotal += entry.getValue();
            }
            System.out.println("-----------------------------");
            System.out.printf("%-12s | %-15d%n", "Grand Total", grandTotal);
        }

        System.out.println();
        System.out.print("Enter Hospital ID to inspect detailed inventory (or press Enter to view all): ");
        String hospInput = scanner.nextLine().trim();

        if (hospInput.isEmpty()) {
            List<Hospital> hospitals = hospitalDAO.getAll();
            for (Hospital h : hospitals) {
                printHospitalInventory(h);
            }
        } else {
            try {
                int hospitalId = Integer.parseInt(hospInput);
                Hospital hospital = hospitalDAO.getById(hospitalId);
                if (hospital == null) {
                    System.out.println("[!] Hospital with ID " + hospitalId + " does not exist.");
                    return;
                }
                printHospitalInventory(hospital);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid Hospital ID format.");
            }
        }
    }

    private static void printHospitalInventory(Hospital hospital) throws SQLException {
        List<BloodStock> stocks = stockDAO.getByHospital(hospital.getHospitalId());
        System.out.println("\nInventory for: " + hospital.getName() + " (" + hospital.getCity() + ", ID: " + hospital.getHospitalId() + ")");
        if (stocks.isEmpty()) {
            System.out.println("  No stock records registered.");
            return;
        }

        System.out.printf("  %-12s | %-10s%n", "Blood Group", "Units");
        System.out.println("  -----------------------");
        int subtotal = 0;
        for (BloodStock bs : stocks) {
            System.out.printf("  %-12s | %-10d%n", bs.getBloodGroup(), bs.getUnitsAvailable());
            subtotal += bs.getUnitsAvailable();
        }
        System.out.println("  -----------------------");
        System.out.printf("  %-12s | %-10d%n", "Hospital Total", subtotal);
    }

    // ------------------------------------------------------------------------
    // Donations, Transactions & Reports (Phase 4: Options 14 - 18)
    // ------------------------------------------------------------------------

    private static void handleRecordDonation(Scanner scanner)
            throws InvalidInputException, NotEligibleException, SQLException {

        System.out.println("\n--- [14] Record Blood Donation ---");

        System.out.print("Enter Donor ID: ");
        int donorId = readInt(scanner, "Donor ID");

        System.out.print("Enter Hospital ID: ");
        int hospitalId = readInt(scanner, "Hospital ID");

        System.out.print("Enter linked Blood Request ID (press Enter if voluntary donation): ");
        String reqStr = scanner.nextLine().trim();
        Integer requestId = null;
        if (!reqStr.isEmpty()) {
            try {
                requestId = Integer.parseInt(reqStr);
            } catch (NumberFormatException e) {
                throw new InvalidInputException("Blood Request ID must be a valid number.");
            }
        }

        System.out.print("Enter units donated (default 1): ");
        String unitsStr = scanner.nextLine().trim();
        int units = unitsStr.isEmpty() ? 1 : readIntFromString(unitsStr, "Units");

        System.out.print("Enter donation date (YYYY-MM-DD, press Enter for today): ");
        String dateStr = scanner.nextLine().trim();
        LocalDate donationDate;
        if (dateStr.isEmpty()) {
            donationDate = LocalDate.now();
        } else {
            donationDate = parseDateOrNull(dateStr);
            if (donationDate == null) {
                donationDate = LocalDate.now();
            }
        }

        int generatedId = donationService.recordDonation(donorId, hospitalId, requestId, units, donationDate);

        System.out.println("[SUCCESS] Donation recorded successfully!");
        System.out.println("  Donation ID : " + generatedId);
        System.out.println("  Donor ID    : " + donorId);
        System.out.println("  Hospital ID : " + hospitalId);
        System.out.println("  Units       : " + units);
        System.out.println("  Date        : " + donationDate);
        System.out.println("--> Database Trigger: Automatically updated donor's last donation date and added units to hospital stock!");
    }

    private static void handleViewDonations(Scanner scanner) throws SQLException, InvalidInputException {
        System.out.println("\n--- [15] View Donation History ---");
        System.out.println("1. View all donations");
        System.out.println("2. View donations by Donor ID");
        System.out.print("Enter choice (1 or 2): ");
        String ch = scanner.nextLine().trim();

        List<Donation> list;
        if (ch.equals("2")) {
            System.out.print("Enter Donor ID: ");
            int donorId = readInt(scanner, "Donor ID");
            list = donationDAO.getByDonor(donorId);
        } else {
            list = donationDAO.getAll();
        }

        if (list.isEmpty()) {
            System.out.println("No donation records found.");
            return;
        }

        System.out.printf("%-6s | %-8s | %-11s | %-10s | %-5s | %-12s%n",
                "DonID", "DonorID", "HospitalID", "RequestID", "Units", "Date");
        System.out.println("---------------------------------------------------------------");
        for (Donation d : list) {
            System.out.printf("%-6d | %-8d | %-11d | %-10s | %-5d | %-12s%n",
                    d.getDonationId(),
                    d.getDonorId(),
                    d.getHospitalId(),
                    (d.getRequestId() != null ? String.valueOf(d.getRequestId()) : "Voluntary"),
                    d.getUnits(),
                    (d.getDonationDate() != null ? d.getDonationDate().toString() : "N/A"));
        }
        System.out.println("Total records: " + list.size());
    }

    private static void handleDeleteDonation(Scanner scanner) throws InvalidInputException, SQLException {
        System.out.println("\n--- [16] Delete Donation Record ---");
        System.out.print("Enter Donation ID to delete: ");
        int donationId = readInt(scanner, "Donation ID");

        Donation donation = donationDAO.getById(donationId);
        if (donation == null) {
            System.out.println("[!] Donation with ID " + donationId + " was not found.");
            return;
        }

        System.out.println("Found: Donation #" + donationId + " | Donor ID: " + donation.getDonorId()
                + " | Hospital ID: " + donation.getHospitalId() + " | Units: " + donation.getUnits()
                + " | Date: " + donation.getDonationDate());

        System.out.print("Are you sure you want to delete this donation record? (Y/N): ");
        String confirm = scanner.nextLine().trim().toUpperCase();

        if (confirm.equals("Y")) {
            boolean deleted = donationDAO.delete(donationId);
            if (deleted) {
                System.out.println("[SUCCESS] Donation #" + donationId + " deleted successfully.");
                System.out.println("--> Database Trigger: Automatically deducted units from hospital stock and recalculated donor's last donation date.");
            } else {
                System.out.println("[!] Failed to delete donation.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private static void handleIssueBlood(Scanner scanner)
            throws InvalidInputException, InsufficientStockException, SQLException {

        System.out.println("\n--- [17] Issue Blood for Request (ACID Transaction) ---");
        System.out.print("Enter Blood Request ID to issue: ");
        int requestId = readInt(scanner, "Request ID");

        BloodRequest req = requestDAO.getById(requestId);
        if (req == null) {
            throw new InvalidInputException("Blood Request with ID " + requestId + " was not found.");
        }

        Hospital hospital = hospitalDAO.getById(req.getHospitalId());
        String hospName = (hospital != null) ? hospital.getName() : ("Hospital #" + req.getHospitalId());

        System.out.println("Request Details:");
        System.out.println("  Request ID   : #" + req.getRequestId());
        System.out.println("  Patient      : " + req.getPatientName());
        System.out.println("  Hospital     : " + hospName);
        System.out.println("  Blood Group  : " + req.getBloodGroup());
        System.out.println("  Units Needed : " + req.getUnitsNeeded());
        System.out.println("  Status       : " + req.getStatus());

        System.out.print("Confirm issuance of " + req.getUnitsNeeded() + " units from hospital inventory? (Y/N): ");
        String confirm = scanner.nextLine().trim().toUpperCase();

        if (!confirm.equals("Y")) {
            System.out.println("Issuance cancelled.");
            return;
        }

        bloodIssueService.issueBlood(requestId);

        System.out.println("[SUCCESS] Blood issued successfully via ACID Database Transaction!");
        System.out.println("  1. Deducted " + req.getUnitsNeeded() + " units of " + req.getBloodGroup() + " from " + hospName + " stock.");
        System.out.println("  2. Request #" + requestId + " status updated to 'FULFILLED'.");
        System.out.println("  3. Transaction committed successfully.");
    }

    private static void handleReportsMenu(Scanner scanner) throws SQLException, InvalidInputException {
        boolean inReports = true;

        while (inReports) {
            System.out.println("\n=================================================");
            System.out.println("             ANALYTICAL REPORTS MENU             ");
            System.out.println("=================================================");
            System.out.println(" 1. Stock per hospital and blood group");
            System.out.println(" 2. Total aggregate stock per blood group");
            System.out.println(" 3. Number of blood requests per status");
            System.out.println(" 4. Monthly donations trend (current year)");
            System.out.println(" 5. Top 5 donors by donations and total units");
            System.out.println(" 6. Donation history of a donor");
            System.out.println(" 0. Back to Main Menu");
            System.out.println("-------------------------------------------------");
            System.out.print("Choose a report (0-6): ");
            String repChoice = scanner.nextLine().trim();

            switch (repChoice) {
                case "1":
                    System.out.println("\n--- [Report A] Stock Per Hospital and Blood Group ---");
                    printReportTable(reportDAO.getStockPerHospitalAndGroup());
                    break;
                case "2":
                    System.out.println("\n--- [Report B] Total Stock Per Blood Group ---");
                    printReportTable(reportDAO.getTotalStockPerBloodGroup());
                    break;
                case "3":
                    System.out.println("\n--- [Report C] Number of Requests Per Status ---");
                    printReportTable(reportDAO.getRequestCountByStatus());
                    break;
                case "4":
                    System.out.println("\n--- [Report D] Monthly Donations Trend (Current Year) ---");
                    printReportTable(reportDAO.getDonationsPerMonthCurrentYear());
                    break;
                case "5":
                    System.out.println("\n--- [Report E] Top 5 Donors by Total Contributions ---");
                    printReportTable(reportDAO.getTop5Donors());
                    break;
                case "6":
                    System.out.print("Enter Donor ID: ");
                    int donorId = readInt(scanner, "Donor ID");
                    Donor donor = donorDAO.getById(donorId);
                    String donorName = (donor != null) ? donor.getName() : ("Donor #" + donorId);
                    System.out.println("\n--- [Report F] Donation History for " + donorName + " (ID: " + donorId + ") ---");
                    printReportTable(reportDAO.getDonationHistoryByDonor(donorId));
                    break;
                case "0":
                    inReports = false;
                    break;
                default:
                    System.out.println("[!] Invalid report choice.");
            }
        }
    }

    private static void printReportTable(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("No data available for this report.");
            return;
        }

        // Extract column names from first row
        Map<String, Object> firstRow = rows.get(0);
        String[] columns = firstRow.keySet().toArray(new String[0]);

        // Compute column widths dynamically
        int[] widths = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            widths[i] = Math.max(columns[i].length(), 10);
            for (Map<String, Object> row : rows) {
                Object val = row.get(columns[i]);
                String valStr = (val != null) ? val.toString() : "-";
                widths[i] = Math.max(widths[i], valStr.length());
            }
        }

        // Print header
        StringBuilder header = new StringBuilder();
        StringBuilder divider = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            String colName = columns[i].replace('_', ' ').toUpperCase();
            header.append(String.format("%-" + widths[i] + "s", colName));
            for (int k = 0; k < widths[i]; k++) divider.append("-");
            if (i < columns.length - 1) {
                header.append(" | ");
                divider.append("-+-");
            }
        }
        System.out.println(header);
        System.out.println(divider);

        // Print rows
        for (Map<String, Object> row : rows) {
            StringBuilder rowStr = new StringBuilder();
            for (int i = 0; i < columns.length; i++) {
                Object val = row.get(columns[i]);
                String valStr = (val != null) ? val.toString() : "-";
                rowStr.append(String.format("%-" + widths[i] + "s", valStr));
                if (i < columns.length - 1) {
                    rowStr.append(" | ");
                }
            }
            System.out.println(rowStr);
        }
        System.out.println("Total rows: " + rows.size());
    }

    // ------------------------------------------------------------------------
    // Safe Console Input Helper Methods
    // ------------------------------------------------------------------------

    private static int readInt(Scanner scanner, String fieldName) throws InvalidInputException {
        String line = scanner.nextLine().trim();
        return readIntFromString(line, fieldName);
    }

    private static int readIntFromString(String line, String fieldName) throws InvalidInputException {
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            throw new InvalidInputException(fieldName + " must be a valid integer number. Entered: '" + line + "'");
        }
    }

    private static LocalDate parseDateOrNull(String dateStr) throws InvalidInputException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Date format must be YYYY-MM-DD (e.g., 2026-05-15). Entered: '" + dateStr + "'");
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 2) + "..";
    }
}
