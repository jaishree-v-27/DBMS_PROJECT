package com.bloodnetwork.dao;

import com.bloodnetwork.model.AppUser;
import com.bloodnetwork.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object (DAO) for the 'app_user' table.
 * Handles user lookup by username and resolves relationships to donor and hospital IDs.
 */
public class UserDAO {

    /**
     * Finds an application user record by username (case-insensitive).
     *
     * @param username The username to look up.
     * @return AppUser object if found, or null otherwise.
     * @throws SQLException if a database error occurs.
     */
    public AppUser findByUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT user_id, username, password_hash, role FROM app_user WHERE LOWER(TRIM(username)) = LOWER(TRIM(?))";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("user_id");
                    String uname = rs.getString("username");
                    String hash = rs.getString("password_hash");
                    String role = rs.getString("role");
                    return new AppUser(id, uname, hash, role);
                }
            }
        }
        return null;
    }

    /**
     * Finds the donor_id associated with a given user_id.
     *
     * @param userId The user ID.
     * @return donor_id if found, or null otherwise.
     * @throws SQLException if a database error occurs.
     */
    public Integer findDonorIdByUserId(int userId) throws SQLException {
        String sql = "SELECT donor_id FROM donor WHERE user_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("donor_id");
                }
            }
        }
        return null;
    }

    /**
     * Finds the hospital_id associated with a given user_id.
     *
     * @param userId The user ID.
     * @return hospital_id if found, or null otherwise.
     * @throws SQLException if a database error occurs.
     */
    public Integer findHospitalIdByUserId(int userId) throws SQLException {
        String sql = "SELECT hospital_id FROM hospital WHERE user_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("hospital_id");
                }
            }
        }
        return null;
    }
}
