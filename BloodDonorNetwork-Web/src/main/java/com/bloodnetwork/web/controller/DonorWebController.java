package com.bloodnetwork.web.controller;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.ReportDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.dao.UserDAO;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.matching.CompatibilityTable;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.util.Validator;
import com.bloodnetwork.web.service.DonorAchievementService;
import com.bloodnetwork.web.service.DonorNotificationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller managing the Voluntary Donor Web Portal.
 * Handles profile inspection, 90-day eligibility ring calculation,
 * verified achievements gallery, in-app notification inbox, and calendar export.
 */
@Controller
public class DonorWebController {

    private final DonorDAO donorDAO = new DonorDAO();
    private final UserDAO userDAO = new UserDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final ReportDAO reportDAO = new ReportDAO();

    private final DonorNotificationService notificationService;
    private final DonorAchievementService achievementService;

    @Autowired
    public DonorWebController(DonorNotificationService notificationService,
                              DonorAchievementService achievementService) {
        this.notificationService = notificationService;
        this.achievementService = achievementService;
    }

    @GetMapping("/donor/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        Integer userId = (Integer) session.getAttribute("userId");

        model.addAttribute("username", username);
        model.addAttribute("role", "DONOR");
        model.addAttribute("roleDisplayName", "Voluntary Donor");
        model.addAttribute("pageTitle", "Voluntary Donor Portal: Blood Donor Network");

        try {
            // 1. Resolve Donor Profile
            Integer donorId = (userId != null) ? userDAO.findDonorIdByUserId(userId) : null;
            if (donorId == null) {
                model.addAttribute("errorMessage", "Donor account not linked.");
                return "auth/login";
            }

            Donor donor = donorDAO.getById(donorId);
            if (donor != null) {
                model.addAttribute("donor", donor);
                model.addAttribute("donorName", donor.getName());
                model.addAttribute("gender", donor.getGender());
                model.addAttribute("bloodGroup", donor.getBloodGroup());
                model.addAttribute("city", donor.getCity());
                model.addAttribute("age", donor.getAge());
                model.addAttribute("phone", donor.getPhone());
                model.addAttribute("available", donor.isAvailable());
                model.addAttribute("donorId", donor.getDonorId());

                // 2. Profile Completeness based on real fields
                int completeness = 0;
                if (donor.getName() != null && !donor.getName().isBlank()) completeness += 20;
                if (donor.getAge() >= 18 && donor.getAge() <= 65) completeness += 20;
                if (donor.getBloodGroup() != null && !donor.getBloodGroup().isBlank()) completeness += 20;
                if (donor.getCity() != null && !donor.getCity().isBlank()) completeness += 20;
                if (donor.getPhone() != null && donor.getPhone().length() == 10) completeness += 20;
                model.addAttribute("profileCompleteness", completeness);

                // 3. Clinical 90-Day Eligibility Evaluation
                boolean isEligible = donor.isEligible(90);
                String eligibilityReason = "You meet all clinical safety criteria and are ready to save a life today.";
                long daysRemaining = 0;
                long daysPassed = 90;
                LocalDate nextDate = null;

                if (donor.getAge() < 18 || donor.getAge() > 65) {
                    eligibilityReason = "Age (" + donor.getAge() + ") falls outside the required 18 to 65 window.";
                } else if (!donor.isAvailable()) {
                    eligibilityReason = "Your profile is currently set to unavailable for donation calls.";
                } else if (donor.getLastDonationDate() != null) {
                    daysPassed = ChronoUnit.DAYS.between(donor.getLastDonationDate(), LocalDate.now());
                    if (daysPassed < 90) {
                        daysRemaining = 90 - daysPassed;
                        nextDate = donor.getLastDonationDate().plusDays(90);
                        eligibilityReason = "90-day recovery active: " + daysRemaining + " days remaining. (Next eligible: " + nextDate + ").";
                    }
                }

                int recoveryPercent = (int) Math.min(100, Math.max(0, (daysPassed * 100) / 90));

                model.addAttribute("isEligible", isEligible);
                model.addAttribute("eligibilityReason", eligibilityReason);
                model.addAttribute("daysRemaining", daysRemaining);
                model.addAttribute("daysPassed", daysPassed);
                model.addAttribute("recoveryPercent", recoveryPercent);
                model.addAttribute("nextEligibleDate", nextDate != null ? nextDate.toString() : "Immediate");
                model.addAttribute("lastDonationDate", donor.getLastDonationDate() != null ? donor.getLastDonationDate().toString() : "Never Donated");

                // 4. Verified Donation History from Database
                List<Map<String, Object>> donationHistory = reportDAO.getDonationHistoryByDonor(donor.getDonorId());
                model.addAttribute("donations", donationHistory);

                int totalUnits = 0;
                for (Map<String, Object> d : donationHistory) {
                    Object u = d.get("units");
                    if (u instanceof Number) totalUnits += ((Number) u).intValue();
                }
                model.addAttribute("totalDonations", donationHistory.size());
                model.addAttribute("totalUnits", totalUnits);

                // 5. Verified Milestone Achievements
                List<DonorAchievementService.MilestoneBadge> badges = achievementService.getBadgesForDonor(donor.getDonorId());
                model.addAttribute("badges", badges);

                // 6. In-App Notifications Inbox ("Requests Near Me")
                List<DonorNotificationService.NotificationItem> notifications = notificationService.getNotificationsForDonor(donor.getDonorId());
                model.addAttribute("notifications", notifications);

                int unreadAlertsCount = 0;
                for (DonorNotificationService.NotificationItem n : notifications) {
                    if ("SENT".equalsIgnoreCase(n.status())) {
                        unreadAlertsCount++;
                    }
                }
                model.addAttribute("unreadAlertsCount", unreadAlertsCount);

                // 7. General Open Matching Demands in Donor's City
                List<BloodRequest> allRequests = requestDAO.getAll();
                List<Map<String, Object>> localMatchingRequests = new ArrayList<>();

                for (BloodRequest req : allRequests) {
                    if (req.getStatus() != RequestStatus.PENDING && req.getStatus() != RequestStatus.MATCHED) {
                        continue;
                    }
                    Hospital h = hospitalDAO.getById(req.getHospitalId());
                    if (h == null || !h.getCity().equalsIgnoreCase(donor.getCity())) {
                        continue;
                    }

                    List<String> compatibleDonorGroups = CompatibilityTable.donorGroupsFor(req.getBloodGroup());
                    if (compatibleDonorGroups.contains(donor.getBloodGroup().trim().toUpperCase())) {
                        localMatchingRequests.add(Map.of(
                                "requestId", req.getRequestId(),
                                "hospitalName", h.getName(),
                                "hospitalCity", h.getCity(),
                                "patientName", req.getPatientName(),
                                "bloodGroup", req.getBloodGroup(),
                                "unitsNeeded", req.getUnitsNeeded(),
                                "urgency", req.getUrgency().name(),
                                "status", req.getStatus().name()
                        ));
                    }
                }
                model.addAttribute("matchingRequests", localMatchingRequests);
            }

        } catch (SQLException e) {
            model.addAttribute("errorMessage", "Database error: " + e.getMessage());
        }

        return "donor/dashboard";
    }

    /**
     * Updates donor contact phone, city, and availability.
     */
    @PostMapping("/donor/profile/update")
    public String updateProfile(@RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam("city") String city,
                                @RequestParam(value = "available", defaultValue = "false") boolean available,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("userId");
        try {
            Integer donorId = (userId != null) ? userDAO.findDonorIdByUserId(userId) : null;
            if (donorId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Donor account not found.");
                return "redirect:/donor/dashboard";
            }

            Validator.validateNonEmpty(name, "Full Name");
            Validator.validatePhone(phone);
            Validator.validateNonEmpty(city, "City");

            Donor d = donorDAO.getById(donorId);
            if (d != null) {
                d.setName(name.trim());
                d.setPhone(phone.trim());
                d.setCity(city.trim());
                d.setAvailable(available);

                donorDAO.update(d);
                redirectAttributes.addFlashAttribute("successMessage", "Your donor profile was successfully updated.");
            }
        } catch (InvalidInputException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (SQLException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Database error: " + e.getMessage());
        }

        return "redirect:/donor/dashboard#profile";
    }

    /**
     * Responds to an in-app transfusion notification (Accept or Decline).
     */
    @PostMapping("/donor/notification/respond")
    public String respondNotification(@RequestParam("notificationId") int notificationId,
                                      @RequestParam("status") String status,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("userId");
        try {
            Integer donorId = (userId != null) ? userDAO.findDonorIdByUserId(userId) : null;
            if (donorId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized action.");
                return "redirect:/donor/dashboard";
            }

            notificationService.respondToNotification(notificationId, donorId, status);
            if ("ACCEPTED".equalsIgnoreCase(status)) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Thank you for volunteering. The requesting hospital has been notified of your acceptance.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Notification response recorded.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error recording response: " + e.getMessage());
        }

        return "redirect:/donor/dashboard#inbox";
    }

    /**
     * Generates a downloadable iCalendar (.ics) file for the next eligible donation date.
     */
    @GetMapping("/donor/reminder.ics")
    public void downloadCalendarReminder(HttpSession session, HttpServletResponse response) throws IOException {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            Integer donorId = userDAO.findDonorIdByUserId(userId);
            Donor donor = (donorId != null) ? donorDAO.getById(donorId) : null;
            if (donor == null || donor.getLastDonationDate() == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No previous donation date recorded.");
                return;
            }

            LocalDate nextEligible = donor.getLastDonationDate().plusDays(90);
            String icsDate = nextEligible.toString().replace("-", "");

            response.setContentType("text/calendar; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"blood_donor_eligibility_" + icsDate + ".ics\"");

            PrintWriter writer = response.getWriter();
            writer.println("BEGIN:VCALENDAR");
            writer.println("VERSION:2.0");
            writer.println("PRODID:-//BloodDonorNetwork//Donor Eligibility Reminder//EN");
            writer.println("BEGIN:VEVENT");
            writer.println("UID:donor-" + donor.getDonorId() + "-" + icsDate + "@blooddonornetwork.org");
            writer.println("DTSTAMP:" + icsDate + "T090000Z");
            writer.println("DTSTART;VALUE=DATE:" + icsDate);
            writer.println("SUMMARY:Eligible to Donate Blood Again (Blood Donor Network)");
            writer.println("DESCRIPTION:Your 90-day clinical recovery period is complete. You are medically eligible to save lives through voluntary blood donation.");
            writer.println("STATUS:CONFIRMED");
            writer.println("END:VEVENT");
            writer.println("END:VCALENDAR");
            writer.flush();

        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database lookup error.");
        }
    }
}
