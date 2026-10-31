package com.bloodnetwork.service;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.UserDAO;
import com.bloodnetwork.exception.AuthenticationException;
import com.bloodnetwork.model.Admin;
import com.bloodnetwork.model.AppUser;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.Requester;
import com.bloodnetwork.model.User;
import com.bloodnetwork.util.PasswordUtil;
import com.bloodnetwork.util.Session;

import java.sql.SQLException;

/**
 * Service handling user authentication and session initialization.
 */
public class AuthService {

    private final UserDAO userDAO;
    private final DonorDAO donorDAO;
    private final HospitalDAO hospitalDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.donorDAO = new DonorDAO();
        this.hospitalDAO = new HospitalDAO();
    }

    public AuthService(UserDAO userDAO, DonorDAO donorDAO, HospitalDAO hospitalDAO) {
        this.userDAO = userDAO;
        this.donorDAO = donorDAO;
        this.hospitalDAO = hospitalDAO;
    }

    /**
     * Authenticates a user against credentials stored in the database.
     * Initializes the global Session upon successful verification.
     *
     * @param username The entered username.
     * @param password The entered plain-text password.
     * @return The authenticated User instance (Admin, Donor, or Requester).
     * @throws AuthenticationException if authentication fails (vague message for security).
     * @throws SQLException if a database error occurs.
     */
    public User login(String username, String password) throws AuthenticationException, SQLException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Invalid username or password.");
        }

        // 1. Look up user record
        AppUser appUser = userDAO.findByUsername(username.trim());
        if (appUser == null) {
            throw new AuthenticationException("Invalid username or password.");
        }

        // 2. Verify hashed password
        boolean valid = PasswordUtil.verify(password, appUser.getPasswordHash());
        if (!valid) {
            throw new AuthenticationException("Invalid username or password.");
        }

        // 3. Resolve role and construct User model
        String role = appUser.getRole().toUpperCase();
        User loggedInUser;
        Integer donorId = null;
        Integer hospitalId = null;

        switch (role) {
            case "ADMIN":
                loggedInUser = new Admin(appUser.getUserId(), appUser.getUsername(), "Administrator", "N/A", "System");
                break;

            case "DONOR":
                donorId = userDAO.findDonorIdByUserId(appUser.getUserId());
                if (donorId != null) {
                    loggedInUser = donorDAO.getById(donorId);
                } else {
                    loggedInUser = new Donor();
                    loggedInUser.setName(appUser.getUsername());
                }
                break;

            case "REQUESTER":
                hospitalId = userDAO.findHospitalIdByUserId(appUser.getUserId());
                String hospName = appUser.getUsername();
                String city = "";
                String phone = "";
                if (hospitalId != null) {
                    Hospital h = hospitalDAO.getById(hospitalId);
                    if (h != null) {
                        hospName = h.getName();
                        city = h.getCity();
                        phone = h.getPhone();
                    }
                }
                loggedInUser = new Requester(appUser.getUserId(), appUser.getUsername(), hospName, phone, city, hospitalId);
                break;

            default:
                throw new AuthenticationException("Invalid username or password.");
        }

        // 4. Start global session
        Session.start(appUser.getUserId(), appUser.getUsername(), role, donorId, hospitalId, loggedInUser);

        return loggedInUser;
    }
}
