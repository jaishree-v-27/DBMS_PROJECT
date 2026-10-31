package com.bloodnetwork.dao;

import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the 'hospital' table.
 * Implements CRUD operations using PreparedStatement and try-with-resources.
 */
public class HospitalDAO {

    /**
     * Inserts a new hospital record and returns the auto-generated hospital_id.
     *
     * @param hospital The hospital object to be inserted.
     * @return The auto-generated hospital_id.
     * @throws SQLException if a database access error occurs.
     */
    public int add(Hospital hospital) throws SQLException {
        String sql = "INSERT INTO hospital (user_id, name, city, phone) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (hospital.getUserId() != null) {
                pstmt.setInt(1, hospital.getUserId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }

            pstmt.setString(2, hospital.getName());
            pstmt.setString(3, hospital.getCity());
            pstmt.setString(4, hospital.getPhone());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    hospital.setHospitalId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating hospital failed; no ID was generated.");
                }
            }
        }
    }

    /**
     * Retrieves a hospital by its primary key (hospital_id).
     *
     * @param hospitalId The hospital ID to find.
     * @return The Hospital object if found, or null otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public Hospital getById(int hospitalId) throws SQLException {
        String sql = "SELECT hospital_id, user_id, name, city, phone FROM hospital WHERE hospital_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, hospitalId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToHospital(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all hospitals registered in the database.
     *
     * @return List of all Hospital objects.
     * @throws SQLException if a database access error occurs.
     */
    public List<Hospital> getAll() throws SQLException {
        List<Hospital> hospitals = new ArrayList<>();
        String sql = "SELECT hospital_id, user_id, name, city, phone FROM hospital ORDER BY hospital_id ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                hospitals.add(mapRowToHospital(rs));
            }
        }
        return hospitals;
    }

    /**
     * Updates an existing hospital's name, city, and phone number.
     *
     * @param hospital The hospital object with updated details.
     * @return true if updated; false if hospitalId not found.
     * @throws SQLException if a database access error occurs.
     */
    public boolean update(Hospital hospital) throws SQLException {
        String sql = "UPDATE hospital SET name = ?, city = ?, phone = ? WHERE hospital_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, hospital.getName());
            pstmt.setString(2, hospital.getCity());
            pstmt.setString(3, hospital.getPhone());
            pstmt.setInt(4, hospital.getHospitalId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Deletes a hospital record by hospital_id.
     *
     * @param hospitalId The ID of the hospital to delete.
     * @return true if deleted; false if not found.
     * @throws SQLException if a database access error occurs.
     */
    public boolean delete(int hospitalId) throws SQLException {
        String sql = "DELETE FROM hospital WHERE hospital_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, hospitalId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Helper method to map a single row of a ResultSet to a Hospital object.
     */
    private Hospital mapRowToHospital(ResultSet rs) throws SQLException {
        int hospitalId = rs.getInt("hospital_id");

        int userIdVal = rs.getInt("user_id");
        Integer userId = rs.wasNull() ? null : userIdVal;

        String name = rs.getString("name");
        String city = rs.getString("city");
        String phone = rs.getString("phone");

        return new Hospital(hospitalId, userId, name, city, phone);
    }
}
