package com.bloodnetwork.service;

import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.dao.StockDAO;
import com.bloodnetwork.exception.InsufficientStockException;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.RequestStatus;
import com.bloodnetwork.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service managing transactional blood issuance from hospital stock to fulfill patient requests.
 */
public class BloodIssueService {

    private final RequestDAO requestDAO;
    private final StockDAO stockDAO;
    private final HospitalDAO hospitalDAO;

    public BloodIssueService() {
        this.requestDAO = new RequestDAO();
        this.stockDAO = new StockDAO();
        this.hospitalDAO = new HospitalDAO();
    }

    public BloodIssueService(RequestDAO requestDAO, StockDAO stockDAO, HospitalDAO hospitalDAO) {
        this.requestDAO = requestDAO;
        this.stockDAO = stockDAO;
        this.hospitalDAO = hospitalDAO;
    }

    /**
     * Issues blood units for a blood request using an ACID database transaction.
     * 
     * Transaction steps:
     * 1. Validate request: must exist and be in PENDING or MATCHED status.
     * 2. Begin transaction: obtain connection and setAutoCommit(false).
     * 3. Call StockDAO.deduct with hospital ID, blood group, and units needed.
     * 4. If deduction returns 0 rows (stock < needed), throw InsufficientStockException.
     * 5. Update request status to FULFILLED using the SAME connection.
     * 6. Commit transaction (conn.commit()).
     * 7. On any error, roll back (conn.rollback()) and rethrow.
     * 8. In finally block, restore autocommit to true and safely close connection.
     *
     * @param requestId The ID of the blood request to fulfill.
     * @throws InvalidInputException if request does not exist or has an invalid status.
     * @throws InsufficientStockException if inventory cannot cover the requested units.
     * @throws SQLException if a database access error occurs.
     */
    public void issueBlood(int requestId)
            throws InvalidInputException, InsufficientStockException, SQLException {

        // 1. Load the request and validate eligibility for issuance
        BloodRequest request = requestDAO.getById(requestId);
        if (request == null) {
            throw new InvalidInputException("Blood request with ID " + requestId + " was not found.");
        }

        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.MATCHED) {
            throw new InvalidInputException("Cannot issue blood: Request #" + requestId + " has status '"
                    + request.getStatus() + "'. Only PENDING or MATCHED requests can be fulfilled.");
        }

        Hospital hospital = hospitalDAO.getById(request.getHospitalId());
        String hospitalName = (hospital != null) ? hospital.getName() : ("Hospital #" + request.getHospitalId());

        Connection conn = null;

        try {
            // 2. Open dedicated transactional connection and turn off autocommit
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 3. Atomically deduct inventory units using the same connection
            int rowsDeducted = stockDAO.deduct(
                    conn,
                    request.getHospitalId(),
                    request.getBloodGroup(),
                    request.getUnitsNeeded()
            );

            // 4. Check if sufficient inventory was available
            if (rowsDeducted == 0) {
                throw new InsufficientStockException("Insufficient stock at " + hospitalName + " ("
                        + (hospital != null ? hospital.getCity() : "") + ") for blood group "
                        + request.getBloodGroup() + ". Required: " + request.getUnitsNeeded() + " units.");
            }

            // 5. Update request status to FULFILLED on the same connection
            boolean statusUpdated = requestDAO.updateStatus(conn, requestId, RequestStatus.FULFILLED.name());
            if (!statusUpdated) {
                throw new SQLException("Failed to update status for Request #" + requestId);
            }

            // 6. Commit transaction
            conn.commit();

        } catch (Exception e) {
            // Roll back on any failure to preserve consistency
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[Rollback Error] " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            // Restore autocommit and close connection
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }
}
