package com.bloodnetwork.web.controller;

import com.bloodnetwork.dao.DonationDAO;
import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.ReportDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.dao.StockDAO;
import com.bloodnetwork.exception.InsufficientStockException;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.exception.NotEligibleException;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.Donation;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.model.Urgency;
import com.bloodnetwork.web.service.DonationRecordingService;
import com.bloodnetwork.web.service.RequestWorkflowService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Controller managing the Administrative Web Portal.
 * Handles system-wide donors, hospitals, demands, stock telemetry,
 * real audit logs, and donation recording.
 */
@Controller
public class AdminWebController {

    private final JdbcTemplate jdbcTemplate;
    private final RequestWorkflowService workflowService;
    private final DonationRecordingService donationRecordingService;

    private final DonorDAO donorDAO = new DonorDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final DonationDAO donationDAO = new DonationDAO();
    private final ReportDAO reportDAO = new ReportDAO();

    @Autowired
    public AdminWebController(JdbcTemplate jdbcTemplate,
                              RequestWorkflowService workflowService,
                              DonationRecordingService donationRecordingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.workflowService = workflowService;
        this.donationRecordingService = donationRecordingService;
    }

    public record RequestListItem(
            int requestId,
            int hospitalId,
            String hospitalName,
            String patientName,
            String bloodGroup,
            int unitsNeeded,
            int allocatableNow,
            Urgency urgency,
            RequestStatus status,
            LocalDateTime requestDate,
            long daysWaiting,
            boolean isCriticalLongWait
    ) {}

    @GetMapping("/admin/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("role", "ADMIN");
        model.addAttribute("roleDisplayName", "Clinical Administrator");
        model.addAttribute("pageTitle", "Administrator Console: Blood Donor Network");

        try {
            // 1. Donors & Eligibility Counts
            List<Donor> donors = donorDAO.getAll();
            int eligibleCount = 0;
            for (Donor d : donors) {
                if (d.isEligible(90)) eligibleCount++;
            }

            // 2. Hospitals
            List<Hospital> hospitals = hospitalDAO.getAll();
            Map<Integer, String> hospitalNameMap = new HashMap<>();
            for (Hospital h : hospitals) {
                hospitalNameMap.put(h.getHospitalId(), h.getName());
            }

            // 3. Requests with Age Calculation
            List<BloodRequest> rawRequests = requestDAO.getAll();
            List<RequestListItem> requests = new ArrayList<>();
            int pendingCount = 0;
            int criticalPendingCount = 0;

            for (BloodRequest r : rawRequests) {
                boolean isOpen = (r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.MATCHED);
                if (isOpen) pendingCount++;

                long daysWaiting = 0;
                if (r.getRequestDate() != null) {
                    daysWaiting = ChronoUnit.DAYS.between(r.getRequestDate().toLocalDate(), LocalDate.now());
                }

                boolean criticalLong = isOpen && r.getUrgency() == Urgency.CRITICAL && daysWaiting >= 1;
                if (criticalLong) criticalPendingCount++;

                // Query allocatable units on-hand
                int allocatable = 0;
                try {
                    Integer avail = jdbcTemplate.queryForObject(
                            "SELECT units_available FROM blood_stock WHERE hospital_id = ? AND blood_group = ?",
                            Integer.class, r.getHospitalId(), r.getBloodGroup()
                    );
                    allocatable = (avail != null) ? Math.min(avail, r.getUnitsNeeded()) : 0;
                } catch (Exception ignored) {}

                requests.add(new RequestListItem(
                        r.getRequestId(),
                        r.getHospitalId(),
                        hospitalNameMap.getOrDefault(r.getHospitalId(), "Hospital #" + r.getHospitalId()),
                        r.getPatientName(),
                        r.getBloodGroup(),
                        r.getUnitsNeeded(),
                        allocatable,
                        r.getUrgency(),
                        r.getStatus(),
                        r.getRequestDate(),
                        daysWaiting,
                        criticalLong
                ));
            }

            // 4. Stock & Inventory
            Map<String, Integer> stockTotals = stockDAO.getTotalByBloodGroup();
            int totalUnits = 0;
            for (int u : stockTotals.values()) totalUnits += u;

            List<Map<String, Object>> stockPerHosp = reportDAO.getStockPerHospitalAndGroup();

            // 5. Donations
            List<Donation> donations = donationDAO.getAll();

            // 6. Top Donors (Ranked by verified donations)
            List<Map<String, Object>> topDonors = reportDAO.getTop5Donors();

            // 7. Recent System Audit Events
            String auditSql = "SELECT e.event_id, e.request_id, e.event_type, e.from_status, e.to_status, "
                            + "       u.username AS actor_username, e.note, e.created_at "
                            + "FROM request_event e "
                            + "LEFT JOIN app_user u ON e.actor_user_id = u.user_id "
                            + "ORDER BY e.created_at DESC LIMIT 20";
            List<Map<String, Object>> recentEvents = jdbcTemplate.queryForList(auditSql);

            model.addAttribute("donors", donors);
            model.addAttribute("eligibleCount", eligibleCount);
            model.addAttribute("hospitals", hospitals);
            model.addAttribute("requests", requests);
            model.addAttribute("donations", donations);
            model.addAttribute("stockPerHosp", stockPerHosp);
            model.addAttribute("topDonors", topDonors);
            model.addAttribute("recentEvents", recentEvents);

            model.addAttribute("totalDonors", donors.size());
            model.addAttribute("totalHospitals", hospitals.size());
            model.addAttribute("pendingRequests", pendingCount);
            model.addAttribute("criticalPendingCount", criticalPendingCount);
            model.addAttribute("unreadAlertsCount", criticalPendingCount);
            model.addAttribute("totalUnits", totalUnits);

        } catch (SQLException e) {
            model.addAttribute("errorMessage", "Error loading database records: " + e.getMessage());
        }

        return "admin/dashboard";
    }

    /**
     * Records a new blood donation session for an eligible voluntary donor.
     * Triggers MySQL after_donation_insert to update donor and stock atomically.
     */
    @PostMapping("/admin/record-donation")
    public String recordDonation(@RequestParam("donorId") int donorId,
                                 @RequestParam("hospitalId") int hospitalId,
                                 @RequestParam(value = "units", defaultValue = "1") int units,
                                 @RequestParam(value = "donationDate", required = false) String dateStr,
                                 RedirectAttributes redirectAttributes) {
        try {
            LocalDate dDate = (dateStr != null && !dateStr.isBlank()) ? LocalDate.parse(dateStr) : LocalDate.now();
            DonationRecordingService.DonationResult res = donationRecordingService.recordDonation(donorId, hospitalId, units, dDate);

            String msg = "Success: Recorded " + units + " unit(s) of " + res.bloodGroup() + " from " 
                       + res.donorName() + " for " + res.hospitalName() + ". Stock credited via trigger.";
            if (!res.newlyAwardedBadges().isEmpty()) {
                msg += " Milestone awarded: " + String.join(", ", res.newlyAwardedBadges()) + "!";
            }
            redirectAttributes.addFlashAttribute("successMessage", msg);

        } catch (NotEligibleException | InvalidInputException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to record donation: " + e.getMessage());
        }

        return "redirect:/admin/dashboard#donations-tab";
    }

    /**
     * Executes atomic ACID blood issuance from inventory to satisfy a patient request.
     */
    @PostMapping("/admin/issue-blood")
    public String issueBlood(@RequestParam("requestId") int requestId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("userId");
        try {
            workflowService.issueBloodAtomic(requestId, userId, "ADMIN");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Transaction Complete: Blood request #" + requestId + " has been successfully fulfilled and inventory deducted.");
        } catch (InsufficientStockException | InvalidInputException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Database transaction error: " + e.getMessage());
        }

        return "redirect:/admin/dashboard#requests-tab";
    }
}
