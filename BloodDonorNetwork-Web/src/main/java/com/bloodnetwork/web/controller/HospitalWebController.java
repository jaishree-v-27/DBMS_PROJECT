package com.bloodnetwork.web.controller;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.dao.StockDAO;
import com.bloodnetwork.dao.UserDAO;
import com.bloodnetwork.exception.AccessDeniedException;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.matching.CompatibilityTable;
import com.bloodnetwork.matching.CompatibleMatcher;
import com.bloodnetwork.matching.ExactMatcher;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.BloodStock;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.model.Urgency;
import com.bloodnetwork.service.RequestService;
import com.bloodnetwork.util.Validator;
import com.bloodnetwork.web.service.DonorNotificationService;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller managing the Hospital Requester Web Portal.
 * Enforces strict facility-level isolation, real-time demand creation,
 * match explanations, and in-app donor notifications.
 */
@Controller
public class HospitalWebController {

    private final JdbcTemplate jdbcTemplate;
    private final RequestWorkflowService workflowService;
    private final DonorNotificationService notificationService;

    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final UserDAO userDAO = new UserDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final RequestService requestService = new RequestService(requestDAO);
    private final ExactMatcher exactMatcher = new ExactMatcher();
    private final CompatibleMatcher compatibleMatcher = new CompatibleMatcher();

    @Autowired
    public HospitalWebController(JdbcTemplate jdbcTemplate,
                                 RequestWorkflowService workflowService,
                                 DonorNotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.workflowService = workflowService;
        this.notificationService = notificationService;
    }

    public record MatchedDonorExplanation(
            Donor donor,
            String strategyUsed,
            boolean isExactGroup,
            String explanationReason
    ) {}

    public record HospitalRequestItem(
            BloodRequest request,
            long daysWaiting,
            int allocatableNow,
            int notifiedCount,
            int acceptedCount
    ) {}

    @GetMapping("/hospital/dashboard")
    public String dashboard(@RequestParam(value = "matchReqId", required = false) Integer matchReqId,
                            @RequestParam(value = "strategy", defaultValue = "compatible") String strategy,
                            HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        Integer userId = (Integer) session.getAttribute("userId");

        model.addAttribute("username", username);
        model.addAttribute("role", "REQUESTER");
        model.addAttribute("pageTitle", "Hospital Clinical Portal: Blood Donor Network");

        try {
            // 1. Resolve Hospital Profile strictly scoped to this user
            Integer hospitalId = (userId != null) ? userDAO.findHospitalIdByUserId(userId) : null;
            if (hospitalId == null) {
                model.addAttribute("errorMessage", "No hospital facility linked to this user account.");
                return "auth/login";
            }

            Hospital hospital = hospitalDAO.getById(hospitalId);
            model.addAttribute("hospital", hospital);
            model.addAttribute("hospitalName", hospital.getName());
            model.addAttribute("hospitalCity", hospital.getCity());
            model.addAttribute("hospitalId", hospital.getHospitalId());
            model.addAttribute("hospitalPhone", hospital.getPhone());
            model.addAttribute("roleDisplayName", "Hospital Staff (" + hospital.getName() + ")");

            // 2. Load Requests belonging ONLY to this Hospital
            List<BloodRequest> rawRequests = requestService.getRequestsForHospital(hospital.getHospitalId());
            List<HospitalRequestItem> requests = new ArrayList<>();
            List<BloodRequest> activeRequestsList = new ArrayList<>();
            int activeCount = 0;

            for (BloodRequest r : rawRequests) {
                if (r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.MATCHED) {
                    activeCount++;
                    activeRequestsList.add(r);
                }

                long daysWaiting = 0;
                if (r.getRequestDate() != null) {
                    daysWaiting = ChronoUnit.DAYS.between(r.getRequestDate().toLocalDate(), LocalDate.now());
                }

                // Query allocatable units on-hand
                int allocatable = 0;
                try {
                    Integer avail = jdbcTemplate.queryForObject(
                            "SELECT units_available FROM blood_stock WHERE hospital_id = ? AND blood_group = ?",
                            Integer.class, hospital.getHospitalId(), r.getBloodGroup()
                    );
                    allocatable = (avail != null) ? Math.min(avail, r.getUnitsNeeded()) : 0;
                } catch (Exception ignored) {}

                // Query notification count
                DonorNotificationService.DonorResponseSummary summary = notificationService.getResponseSummaryForRequest(r.getRequestId());

                requests.add(new HospitalRequestItem(
                        r,
                        daysWaiting,
                        allocatable,
                        summary.totalSent(),
                        summary.totalAccepted()
                ));
            }

            model.addAttribute("requests", requests);
            model.addAttribute("activeRequests", activeCount);
            model.addAttribute("activeRequestsList", activeRequestsList);
            model.addAttribute("unreadAlertsCount", activeCount);

            // 3. Load Blood Stock for this Hospital across all 8 standard blood groups
            List<BloodStock> stockList = stockDAO.getByHospital(hospital.getHospitalId());
            Map<String, Integer> stockMap = new HashMap<>();
            for (BloodStock s : stockList) {
                stockMap.put(s.getBloodGroup(), s.getUnitsAvailable());
            }

            int totalStock = 0;
            for (int u : stockMap.values()) totalStock += u;
            model.addAttribute("hospitalUnits", totalStock);
            model.addAttribute("stockList", stockList);
            model.addAttribute("stockMap", stockMap);
            model.addAttribute("orderedBloodGroups", List.of("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"));

            // 4. If a match lookup was triggered (or default to first active demand), run matching strategy
            Integer targetReqId = matchReqId;
            if (targetReqId == null && !activeRequestsList.isEmpty()) {
                targetReqId = activeRequestsList.get(0).getRequestId();
            }

            if (targetReqId != null) {
                BloodRequest reqToMatch = requestDAO.getById(targetReqId);
                if (reqToMatch != null && reqToMatch.getHospitalId() == hospital.getHospitalId()) {
                    List<Donor> matches;
                    if ("exact".equalsIgnoreCase(strategy)) {
                        matches = exactMatcher.findMatches(reqToMatch);
                    } else {
                        matches = compatibleMatcher.findMatches(reqToMatch);
                    }

                    List<MatchedDonorExplanation> explainedMatches = new ArrayList<>();
                    for (Donor d : matches) {
                        boolean exact = d.getBloodGroup().equalsIgnoreCase(reqToMatch.getBloodGroup());
                        String reason;
                        if (exact) {
                            reason = "Identical blood group (" + d.getBloodGroup() + ") in " + d.getCity() + ". Eligible (last donated: " 
                                   + (d.getLastDonationDate() != null ? d.getLastDonationDate() : "Never") + ").";
                        } else {
                            reason = "Antigen-compatible group (" + d.getBloodGroup() + ") for recipient (" + reqToMatch.getBloodGroup() 
                                   + ") in " + d.getCity() + ". Eligible.";
                        }
                        explainedMatches.add(new MatchedDonorExplanation(d, strategy.toUpperCase(), exact, reason));
                    }

                    model.addAttribute("matchedRequest", reqToMatch);
                    model.addAttribute("explainedMatches", explainedMatches);
                    model.addAttribute("matchStrategy", strategy);

                    // Also fetch response summary for this request
                    DonorNotificationService.DonorResponseSummary summary = notificationService.getResponseSummaryForRequest(targetReqId);
                    model.addAttribute("notifSummary", summary);
                }
            }

        } catch (SQLException e) {
            model.addAttribute("errorMessage", "Database error: " + e.getMessage());
        }

        return "hospital/dashboard";
    }

    /**
     * Submits a new blood request for a patient from this hospital.
     */
    @PostMapping("/hospital/request/new")
    public String raiseRequest(@RequestParam("patientName") String patientName,
                               @RequestParam("bloodGroup") String bloodGroup,
                               @RequestParam("unitsNeeded") int unitsNeeded,
                               @RequestParam("urgency") String urgencyStr,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("userId");
        try {
            Integer hospitalId = (userId != null) ? userDAO.findHospitalIdByUserId(userId) : null;
            if (hospitalId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "No hospital profile linked to your user account.");
                return "redirect:/hospital/dashboard";
            }

            Validator.validateNonEmpty(patientName, "Patient name");
            Validator.validateBloodGroup(bloodGroup);
            if (unitsNeeded <= 0 || unitsNeeded > 20) {
                throw new InvalidInputException("Units needed must be between 1 and 20.");
            }

            Urgency urgency = Urgency.valueOf(urgencyStr.toUpperCase());
            BloodRequest request = new BloodRequest(hospitalId, patientName.trim(), bloodGroup, unitsNeeded, urgency);

            int newId = requestDAO.add(request);
            request.setRequestId(newId);

            // Record initial audit event
            workflowService.recordEvent(
                    newId,
                    "CREATED",
                    null,
                    RequestStatus.PENDING.name(),
                    userId,
                    "Demand raised by hospital staff for patient " + patientName.trim() + " (" + unitsNeeded + " units of " + bloodGroup + ", Urgency: " + urgency + ")."
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    "Emergency blood demand #" + request.getRequestId() + " successfully created.");

        } catch (InvalidInputException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit request: " + e.getMessage());
        }

        return "redirect:/hospital/dashboard#demands";
    }

    /**
     * Cancels an open blood request.
     */
    @PostMapping("/hospital/request/cancel")
    public String cancelRequest(@RequestParam("requestId") int requestId,
                                @RequestParam(value = "reason", required = false) String reason,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("userId");
        try {
            Integer hospitalId = (userId != null) ? userDAO.findHospitalIdByUserId(userId) : null;
            workflowService.cancelRequest(requestId, userId, hospitalId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Request #" + requestId + " has been cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/hospital/dashboard#demands";
    }

    /**
     * Notifies a matched voluntary donor via in-app notification.
     */
    @PostMapping("/hospital/request/notify")
    public String notifyDonor(@RequestParam("requestId") int requestId,
                              @RequestParam("donorId") int donorId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("userId");
        try {
            notificationService.sendNotification(requestId, donorId, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "In-app notification dispatched to donor #" + donorId + ". Check responses in the match panel.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/hospital/dashboard?matchReqId=" + requestId + "#matching";
    }
}
