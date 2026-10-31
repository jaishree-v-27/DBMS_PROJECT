package com.bloodnetwork.service;

import com.bloodnetwork.dao.UserDAO;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.util.DBConnection;
import com.bloodnetwork.util.PasswordUtil;
import com.bloodnetwork.util.Validator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service managing user registrations via single atomic database transactions.
 */
public class RegistrationService {

    private final UserDAO userDAO;

    public RegistrationService() {
        this.userDAO = new UserDAO();
    }

    public RegistrationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Registers a new donor account using an ACID database transaction.
     * Inserts into 'app_user' with role DONOR, then inserts into 'donor' linked by user_id.
     * Rolls back on any error to avoid orphaned accounts.
     *
     * @param username Desired login username.
     * @param password Plain-text password (minimum 6 characters).
     * @param confirmPassword Confirmation password.
     * @param name Full name.
     * @param age Age (18 to 65).
     * @param gender Gender ('M', 'F', 'O').
     * @param bloodGroup Blood group ('A+', etc.).
     * @param phone 10-digit phone number.
     * @param city Residence city.
     * @return Generated donor ID.
     * @throws InvalidInputException if validation fails.
     * @throws SQLException if a database error occurs.
     */
    public int registerDonor(String username, String password, String confirmPassword,
                              String name, int age, String gender, String bloodGroup,
                              String phone, String city) throws InvalidInputException, SQLException {

        // 1. Username validations
        Validator.validateNonEmpty(username, "Username");
        if (username.trim().length() < 3) {
            throw new InvalidInputException("Username must be at least 3 characters long.");
        }

        // Check duplicate username
        if (userDAO.findByUsername(username.trim()) != null) {
            throw new InvalidInputException("Username '" + username.trim() + "' is already taken. Please choose another username.");
        }

        // 2. Password validations
        if (password == null || password.length() < 6) {
            throw new InvalidInputException("Password must be at least 6 characters long.");
        }
        if (!password.equals(confirmPassword)) {
            throw new InvalidInputException("Passwords do not match. Please re-enter your password.");
        }

        // 3. Demographic and medical validations
        Validator.validateNonEmpty(name, "Full Name");
        Validator.validateAge(age);

        if (gender == null || (!gender.equalsIgnoreCase("M") && !gender.equalsIgnoreCase("F") && !gender.equalsIgnoreCase("O"))) {
            throw new InvalidInputException("Gender must be 'M', 'F', or 'O'.");
        }

        Validator.validateBloodGroup(bloodGroup);
        Validator.validatePhone(phone);
        Validator.validateNonEmpty(city, "City");

        // 4. Execute atomic transaction
        Connection conn = null;

        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // Step A: Insert into app_user
            String sqlUser = "INSERT INTO app_user (username, password_hash, role) VALUES (?, ?, 'DONOR')";
            int userId;

            try (PreparedStatement pstmtUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                pstmtUser.setString(1, username.trim());
                pstmtUser.setString(2, PasswordUtil.hash(password));
                pstmtUser.executeUpdate();

                try (ResultSet rs = pstmtUser.getGeneratedKeys()) {
                    if (rs.next()) {
                        userId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to create user account; no user ID generated.");
                    }
                }
            }

            // Step B: Insert into donor with the new user_id
            String sqlDonor = "INSERT INTO donor (user_id, name, age, gender, blood_group, phone, city, last_donation_date, available) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, TRUE)";
            int donorId;

            try (PreparedStatement pstmtDonor = conn.prepareStatement(sqlDonor, Statement.RETURN_GENERATED_KEYS)) {
                pstmtDonor.setInt(1, userId);
                pstmtDonor.setString(2, name.trim());
                pstmtDonor.setInt(3, age);
                pstmtDonor.setString(4, gender.trim().toUpperCase());
                pstmtDonor.setString(5, bloodGroup.trim().toUpperCase());
                pstmtDonor.setString(6, phone.trim());
                pstmtDonor.setString(7, city.trim());
                pstmtDonor.executeUpdate();

                try (ResultSet rs = pstmtDonor.getGeneratedKeys()) {
                    if (rs.next()) {
                        donorId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to create donor profile; no donor ID generated.");
                    }
                }
            }

            // Step C: Commit transaction
            conn.commit();
            return donorId;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[Registration Rollback Error] " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }
}
