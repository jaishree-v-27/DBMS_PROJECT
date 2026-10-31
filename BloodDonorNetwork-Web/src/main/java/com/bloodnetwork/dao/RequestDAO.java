package com.bloodnetwork.dao;

import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.model.Urgency;
import com.bloodnetwork.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the 'blood_request' table.
 * Provides operations to create, query, prioritize, update, and cancel requests.
 */
public class RequestDAO {

    /**
     * Inserts a new blood request into the database. Status defaults to PENDING.
     *
     * @param request The BloodRequest to insert.
     * @return Auto-generated request_id.
     * @throws SQLException if a database error occurs.
     */
    public int add(BloodRequest request) throws SQLException {
        String sql = "INSERT INTO blood_request (hospital_id, patient_name, blood_group, units_needed, urgency, status, request_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, request.getHospitalId());
            pstmt.setString(2, request.getPatientName());
            pstmt.setString(3, request.getBloodGroup());
            pstmt.setInt(4, request.getUnitsNeeded());

            String urgencyStr = (request.getUrgency() != null) ? request.getUrgency().name() : Urgency.NORMAL.name();
            pstmt.setString(5, urgencyStr);

            // Status defaults to PENDING
            String statusStr = (request.getStatus() != null) ? request.getStatus().name() : RequestStatus.PENDING.name();
            pstmt.setString(6, statusStr);

            Timestamp timestamp = (request.getRequestDate() != null)
                    ? Timestamp.valueOf(request.getRequestDate())
                    : new Timestamp(System.currentTimeMillis());
            pstmt.setTimestamp(7, timestamp);

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    request.setRequestId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating blood request failed; no ID was generated.");
                }
            }
        }
    }

    /**
     * Retrieves a blood request by its primary key (request_id).
     *
     * @param requestId The request ID to look up.
     * @return The BloodRequest object if found, or null otherwise.
     * @throws SQLException if a database error occurs.
     */
    public BloodRequest getById(int requestId) throws SQLException {
        String sql = "SELECT request_id, hospital_id, patient_name, blood_group, units_needed, urgency, status, request_date "
                   + "FROM blood_request WHERE request_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, requestId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRequest(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all blood requests in the database.
     *
     * @return List of all BloodRequests ordered by request_id.
     * @throws SQLException if a database error occurs.
     */
    public List<BloodRequest> getAll() throws SQLException {
        List<BloodRequest> requests = new ArrayList<>();
        String sql = "SELECT request_id, hospital_id, patient_name, blood_group, units_needed, urgency, status, request_date "
                   + "FROM blood_request ORDER BY request_id ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                requests.add(mapRowToRequest(rs));
            }
        }
        return requests;
    }

    /**
     * Retrieves all blood requests raised by a specific hospital.
     *
     * @param hospitalId The hospital ID.
     * @return List of requests for that hospital.
     * @throws SQLException if a database error occurs.
     */
    public List<BloodRequest> getByHospital(int hospitalId) throws SQLException {
        List<BloodRequest> requests = new ArrayList<>();
        String sql = "SELECT request_id, hospital_id, patient_name, blood_group, units_needed, urgency, status, request_date "
                   + "FROM blood_request WHERE hospital_id = ? ORDER BY request_date DESC, request_id DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, hospitalId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRowToRequest(rs));
                }
            }
        }
        return requests;
    }

    /**
     * Retrieves all blood requests matching a specific status.
     *
     * @param status The RequestStatus filter (e.g. PENDING, MATCHED, FULFILLED, CANCELLED).
     * @return List of matching requests.
     * @throws SQLException if a database error occurs.
     */
    public List<BloodRequest> getByStatus(RequestStatus status) throws SQLException {
        List<BloodRequest> requests = new ArrayList<>();
        String sql = "SELECT request_id, hospital_id, patient_name, blood_group, units_needed, urgency, status, request_date "
                   + "FROM blood_request WHERE status = ? ORDER BY request_date DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapRowToRequest(rs));
                }
            }
        }
        return requests;
    }

    /**
     * Retrieves all PENDING requests prioritized by urgency (CRITICAL first, then HIGH, then NORMAL),
     * and within each urgency level ordered by oldest first (request_date ASC).
     *
     * @return Sorted list of pending requests.
     * @throws SQLException if a database error occurs.
     */
    public List<BloodRequest> getPendingByUrgency() throws SQLException {
        List<BloodRequest> requests = new ArrayList<>();
        String sql = "SELECT request_id, hospital_id, patient_name, blood_group, units_needed, urgency, status, request_date "
                   + "FROM blood_request WHERE status = 'PENDING' "
                   + "ORDER BY FIELD(urgency, 'CRITICAL', 'HIGH', 'NORMAL'), request_date ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                requests.add(mapRowToRequest(rs));
            }
        }
        return requests;
    }

    /**
     * Updates the status of an existing blood request using an active transactional connection.
     *
     * @param conn Active transactional connection.
     * @param requestId ID of the request to update.
     * @param status New status string (e.g. 'FULFILLED', 'MATCHED', 'PENDING', 'CANCELLED').
     * @return true if updated; false if request not found.
     * @throws SQLException if a database error occurs.
     */
    public boolean updateStatus(Connection conn, int requestId, String status) throws SQLException {
        String sql = "UPDATE blood_request SET status = ? WHERE request_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, requestId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Updates the status of an existing blood request.
     *
     * @param requestId ID of the request to update.
     * @param newStatus New RequestStatus to assign.
     * @return true if updated; false if request not found.
     * @throws SQLException if a database error occurs.
     */
    public boolean updateStatus(int requestId, RequestStatus newStatus) throws SQLException {
        String sql = "UPDATE blood_request SET status = ? WHERE request_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, requestId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Updates the number of units needed for an existing blood request.
     *
     * @param requestId ID of the request.
     * @param newUnits The updated unit requirement (must be > 0).
     * @return true if updated; false if request not found.
     * @throws SQLException if a database error occurs.
     */
    public boolean updateUnits(int requestId, int newUnits) throws SQLException {
        String sql = "UPDATE blood_request SET units_needed = ? WHERE request_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newUnits);
            pstmt.setInt(2, requestId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Cancels an existing blood request by updating its status to 'CANCELLED'.
     * Rows are preserved in the database for auditing and historical records.
     *
     * @param requestId ID of the request to cancel.
     * @return true if cancelled; false if request not found.
     * @throws SQLException if a database error occurs.
     */
    public boolean cancel(int requestId) throws SQLException {
        return updateStatus(requestId, RequestStatus.CANCELLED);
    }

    /**
     * Helper method to map a ResultSet row to a BloodRequest entity.
     */
    private BloodRequest mapRowToRequest(ResultSet rs) throws SQLException {
        int requestId = rs.getInt("request_id");
        int hospitalId = rs.getInt("hospital_id");
        String patientName = rs.getString("patient_name");
        String bloodGroup = rs.getString("blood_group");
        int unitsNeeded = rs.getInt("units_needed");

        Urgency urgency = Urgency.valueOf(rs.getString("urgency"));
        RequestStatus status = RequestStatus.valueOf(rs.getString("status"));

        Timestamp ts = rs.getTimestamp("request_date");
        LocalDateTime requestDate = (ts != null) ? ts.toLocalDateTime() : null;

        return new BloodRequest(requestId, hospitalId, patientName, bloodGroup, unitsNeeded, urgency, status, requestDate);
    }
}
