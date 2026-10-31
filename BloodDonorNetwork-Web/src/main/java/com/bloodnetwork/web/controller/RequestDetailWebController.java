package com.bloodnetwork.web.controller;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.dao.StockDAO;
import com.bloodnetwork.dao.UserDAO;
import com.bloodnetwork.exception.InsufficientStockException;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.matching.CompatibleMatcher;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.web.service.DonorNotificationService;
import com.bloodnetwork.web.service.RequestWorkflowService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Controller managing the detailed Blood Request View,
 * smart allocation plans, real event history, and fulfillment actions.
 */
@Controller
public class RequestDetailWebController {

    private final RequestDAO requestDAO = new RequestDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final DonorDAO donorDAO = new DonorDAO();
    private final UserDAO userDAO = new UserDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final RequestWorkflowService workflowService;
    private final DonorNotificationService notificationService;
    private final CompatibleMatcher compatibleMatcher = new CompatibleMatcher();

    @Autowired
    public RequestDetailWebController(RequestWorkflowService workflowService,
                                     DonorNotificationService notificationService) {
        this.workflowService = workflowService;
        this.notificationService = notificationService;
    }

    @GetMapping("/request/{id}")
    public String requestDetails(@PathVariable("id") int requestId,
                                 HttpSession session,
                                 Model model) {
        String role = (String) session.getAttribute("role");
        Integer userId = (Integer) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        if (role == null || (!"ADMIN".equalsIgnoreCase(role) && !"REQUESTER".equalsIgnoreCase(role))) {
            return "redirect:/login?error=forbidden";
        }

        try {
            BloodRequest request = requestDAO.getById(requestId);
            if (request == null) {
                model.addAttribute("errorMessage", "Request #" + requestId + " was not found.");
                return "error/404";
            }

            // Multi-tenant authorization check for hospital users
            Integer hospitalId = null;
            if ("REQUESTER".equalsIgnoreCase(role)) {
                hospitalId = userDAO.findHospitalIdByUserId(userId);
                if (hospitalId == null || hospitalId != request.getHospitalId()) {
                    return "redirect:/hospital/dashboard?error=forbidden";
                }
            }

            Hospital hospital = hospitalDAO.getById(request.getHospitalId());

            // 1. Calculate real age (waiting time) in days
            long daysWaiting = 0;
            if (request.getRequestDate() != null) {
                daysWaiting = ChronoUnit.DAYS.between(request.getRequestDate().toLocalDate(), LocalDate.now());
            }

            // 2. Compute smart allocation plan
            RequestWorkflowService.FulfilmentPlan plan = workflowService.computeSmartAllocationPlan(
                    request.getHospitalId(),
                    request.getRequestId(),
                    request.getBloodGroup(),
                    request.getUnitsNeeded()
            );

            // 3. Load real audit event log
            List<RequestWorkflowService.RequestEventItem> events = workflowService.getEventsForRequest(requestId);

            // 4. Find compatible voluntary donors in the same city
            List<Donor> matchedDonors = compatibleMatcher.findMatches(request);

            // 5. Load in-app notification responses
            DonorNotificationService.DonorResponseSummary responseSummary = notificationService.getResponseSummaryForRequest(requestId);

            model.addAttribute("request", request);
            model.addAttribute("hospital", hospital);
            model.addAttribute("daysWaiting", daysWaiting);
            model.addAttribute("plan", plan);
            model.addAttribute("events", events);
            model.addAttribute("matchedDonors", matchedDonors);
            model.addAttribute("responseSummary", responseSummary);
            model.addAttribute("username", username);
            model.addAttribute("role", role);
            model.addAttribute("pageTitle", "Blood Request #" + requestId + ": Blood Donor Network");

            return "request/detail";

        } catch (SQLException e) {
            model.addAttribute("errorMessage", "Database error: " + e.getMessage());
            return "redirect:/";
        }
    }

    /**
     * Issues blood atomically using SELECT ... FOR UPDATE row locking.
     */
    @PostMapping("/request/{id}/issue")
    public String issueBlood(@PathVariable("id") int requestId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        Integer userId = (Integer) session.getAttribute("userId");

        if (role == null || (!"ADMIN".equalsIgnoreCase(role) && !"REQUESTER".equalsIgnoreCase(role))) {
            return "redirect:/login?error=forbidden";
        }

        try {
            workflowService.issueBloodAtomic(requestId, userId, role);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Transaction committed: Blood request #" + requestId + " successfully fulfilled and stock deducted.");
        } catch (InsufficientStockException | InvalidInputException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Issuance failed: " + e.getMessage());
        }

        return "redirect:/request/" + requestId;
    }

    /**
     * Cancels an eligible blood request.
     */
    @PostMapping("/request/{id}/cancel")
    public String cancelRequest(@PathVariable("id") int requestId,
                                @RequestParam(value = "reason", required = false) String reason,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        Integer userId = (Integer) session.getAttribute("userId");

        try {
            Integer hospitalScope = null;
            if ("REQUESTER".equalsIgnoreCase(role)) {
                hospitalScope = userDAO.findHospitalIdByUserId(userId);
            }

            workflowService.cancelRequest(requestId, userId, hospitalScope, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Blood demand #" + requestId + " has been cancelled.");
        } catch (InvalidInputException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cancellation error: " + e.getMessage());
        }

        return "redirect:/request/" + requestId;
    }

    /**
     * Dispatches in-app notification to a compatible voluntary donor.
     */
    @PostMapping("/request/{id}/notify-donor")
    public String notifyDonor(@PathVariable("id") int requestId,
                              @RequestParam("donorId") int donorId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("userId");
        try {
            notificationService.sendNotification(requestId, donorId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "In-app transfusion notification dispatched to donor #" + donorId + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not send notification: " + e.getMessage());
        }

        return "redirect:/request/" + requestId;
    }
}
