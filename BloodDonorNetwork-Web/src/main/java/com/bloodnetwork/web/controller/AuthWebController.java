package com.bloodnetwork.web.controller;

import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.service.RegistrationService;
import com.bloodnetwork.web.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Controller handling user authentication requests (GET /login, POST /login, GET /logout).
 * Enforces brute-force lockout, BCrypt upgrades, session fixation protection,
 * and role-based dispatching.
 */
@Controller
public class AuthWebController {

    private final JdbcTemplate jdbcTemplate;
    private final SecurityService securityService;
    private final RegistrationService registrationService;

    @Value("${app.demo-mode:true}")
    private boolean demoMode;

    @Autowired
    public AuthWebController(JdbcTemplate jdbcTemplate, SecurityService securityService) {
        this.jdbcTemplate = jdbcTemplate;
        this.securityService = securityService;
        this.registrationService = new RegistrationService();
    }

    /**
     * Renders the donor registration page (GET /register).
     */
    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        if (session != null && session.getAttribute("role") != null) {
            String role = (String) session.getAttribute("role");
            if ("ADMIN".equalsIgnoreCase(role)) return "redirect:/admin/dashboard";
            if ("REQUESTER".equalsIgnoreCase(role)) return "redirect:/hospital/dashboard";
            if ("DONOR".equalsIgnoreCase(role)) return "redirect:/donor/dashboard";
        }

        model.addAttribute("pageTitle", "Voluntary Donor Registration: Blood Donor Network");
        return "auth/register";
    }

    /**
     * Handles donor registration submissions (POST /register).
     */
    @PostMapping("/register")
    public String handleRegister(@RequestParam(value = "username", required = false) String username,
                                 @RequestParam(value = "password", required = false) String password,
                                 @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                 @RequestParam(value = "name", required = false) String name,
                                 @RequestParam(value = "age", defaultValue = "0") int age,
                                 @RequestParam(value = "gender", required = false) String gender,
                                 @RequestParam(value = "bloodGroup", required = false) String bloodGroup,
                                 @RequestParam(value = "phone", required = false) String phone,
                                 @RequestParam(value = "city", required = false) String city,
                                 Model model,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        model.addAttribute("pageTitle", "Voluntary Donor Registration: Blood Donor Network");

        try {
            registrationService.registerDonor(
                username, password, confirmPassword, name, age, gender, bloodGroup, phone, city
            );
            redirectAttributes.addFlashAttribute("logoutMessage", "Registration successful! Welcome to the Voluntary Donor Network. Please sign in with your credentials.");
            return "redirect:/login";
        } catch (InvalidInputException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("name", name);
            model.addAttribute("age", age > 0 ? age : "");
            model.addAttribute("gender", gender);
            model.addAttribute("bloodGroup", bloodGroup);
            model.addAttribute("phone", phone);
            model.addAttribute("city", city);
            return "auth/register";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Registration failed: " + ex.getMessage());
            return "auth/register";
        }
    }

    /**
     * Renders the login page with status and error alerts.
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            HttpSession session,
                            Model model) {

        // If already authenticated, redirect to role dashboard
        if (session != null && session.getAttribute("role") != null) {
            String role = (String) session.getAttribute("role");
            if ("ADMIN".equalsIgnoreCase(role)) return "redirect:/admin/dashboard";
            if ("REQUESTER".equalsIgnoreCase(role)) return "redirect:/hospital/dashboard";
            if ("DONOR".equalsIgnoreCase(role)) return "redirect:/donor/dashboard";
        }

        model.addAttribute("pageTitle", "Clinical Sign In: Blood Donor Network");
        model.addAttribute("demoMode", demoMode);

        if (error != null) {
            if ("unauthorized".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "Authentication required: Please sign in to access that protected portal.");
            } else if ("forbidden".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "Access denied: Your account does not have permission to view that portal.");
            } else {
                model.addAttribute("errorMessage", "Invalid username or password. Please verify your credentials.");
            }
        }

        if (logout != null) {
            model.addAttribute("logoutMessage", "You have successfully signed out of the clinical session.");
        }

        return "auth/login";
    }

    /**
     * Handles login form submissions (POST /login).
     */
    @PostMapping("/login")
    public String handleLogin(@RequestParam(value = "username", required = false) String username,
                              @RequestParam(value = "password", required = false) String password,
                              HttpServletRequest request,
                              Model model) {
        model.addAttribute("pageTitle", "Clinical Sign In: Blood Donor Network");
        model.addAttribute("demoMode", demoMode);

        // 1. Validate required fields
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            model.addAttribute("errorMessage", "Both username and password are required.");
            return "auth/login";
        }

        String trimmedUser = username.trim();

        // 2. Check for brute-force lockout
        if (securityService.isLockedOut(trimmedUser)) {
            long remainingMins = securityService.getRemainingLockoutMinutes(trimmedUser);
            model.addAttribute("errorMessage", "Account temporarily locked out due to multiple failed login attempts. Please retry in " + remainingMins + " minutes.");
            return "auth/login";
        }

        try {
            // 3. Query user by username
            String sql = "SELECT user_id, username, password_hash, role FROM app_user WHERE LOWER(TRIM(username)) = LOWER(TRIM(?))";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, trimmedUser);

            if (users.isEmpty()) {
                securityService.recordFailedAttempt(trimmedUser);
                model.addAttribute("errorMessage", "Invalid username or password. Please verify your credentials.");
                return "auth/login";
            }

            Map<String, Object> user = users.get(0);
            String storedHash = (String) user.get("password_hash");
            String role = (String) user.get("role");
            String dbUsername = (String) user.get("username");
            int userId = ((Number) user.get("user_id")).intValue();

            // 4. Verify password with BCrypt (and transparent upgrade if legacy SHA-256)
            boolean valid = securityService.verifyAndUpgradePassword(userId, password, storedHash);
            if (!valid) {
                securityService.recordFailedAttempt(trimmedUser);
                model.addAttribute("errorMessage", "Invalid username or password. Please verify your credentials.");
                return "auth/login";
            }

            // Successful authentication: clear failure count
            securityService.recordSuccessfulLogin(trimmedUser);

            // 5. Session fixation protection: invalidate old session and create new
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("userId", userId);
            newSession.setAttribute("username", dbUsername);
            newSession.setAttribute("role", role);

            // 6. Navigate to role-specific dashboard
            if ("ADMIN".equalsIgnoreCase(role)) {
                return "redirect:/admin/dashboard";
            } else if ("REQUESTER".equalsIgnoreCase(role)) {
                return "redirect:/hospital/dashboard";
            } else if ("DONOR".equalsIgnoreCase(role)) {
                return "redirect:/donor/dashboard";
            } else {
                return "redirect:/";
            }

        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Database authentication unavailable: " + ex.getMessage());
            return "auth/login";
        }
    }

    /**
     * Terminates active session and redirects to login with confirmation.
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }

    /**
     * Handles password change requests for all authenticated roles.
     */
    @PostMapping("/account/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 HttpSession session,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (session == null || session.getAttribute("userId") == null) {
            return "redirect:/login?error=unauthorized";
        }

        int userId = (Integer) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        String returnDash = "redirect:/";
        if ("ADMIN".equalsIgnoreCase(role)) returnDash = "redirect:/admin/dashboard#settings";
        else if ("REQUESTER".equalsIgnoreCase(role)) returnDash = "redirect:/hospital/dashboard#settings";
        else if ("DONOR".equalsIgnoreCase(role)) returnDash = "redirect:/donor/dashboard#settings";

        if (newPassword == null || newPassword.trim().length() < 4) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password must be at least 4 characters.");
            return returnDash;
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirmation do not match.");
            return returnDash;
        }

        boolean updated = securityService.changePassword(userId, currentPassword, newPassword);
        if (updated) {
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully with BCrypt encryption.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Current password was incorrect. Please try again.");
        }

        return returnDash;
    }
}
