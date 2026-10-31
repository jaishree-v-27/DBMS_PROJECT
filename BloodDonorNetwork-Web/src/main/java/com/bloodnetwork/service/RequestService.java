package com.bloodnetwork.service;

import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.exception.AccessDeniedException;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.RequestStatus;

import java.sql.SQLException;
import java.util.List;

/**
 * Service enforcing authorization and business rules on blood requests.
 * Ensures hospital requesters can only inspect and manage requests originating
 * from their own institution.
 */
public class RequestService {

    private final RequestDAO requestDAO;

    public RequestService() {
        this.requestDAO = new RequestDAO();
    }

    public RequestService(RequestDAO requestDAO) {
        this.requestDAO = requestDAO;
    }

    /**
     * Retrieves all requests raised by a specific hospital.
     *
     * @param hospitalId The ID of the hospital.
     * @return List of requests belonging to that hospital.
     * @throws SQLException if a database error occurs.
     */
    public List<BloodRequest> getRequestsForHospital(int hospitalId) throws SQLException {
        return requestDAO.getByHospital(hospitalId);
    }

    /**
     * Cancels a request ensuring the calling hospital is the true owner.
     *
     * @param requestId The ID of the request to cancel.
     * @param hospitalId The ID of the hospital requesting cancellation.
     * @return true if successfully cancelled.
     * @throws AccessDeniedException if the request belongs to another hospital.
     * @throws InvalidInputException if the request does not exist or has an un-cancellable status.
     * @throws SQLException if a database error occurs.
     */
    public boolean cancelOwnRequest(int requestId, int hospitalId)
            throws AccessDeniedException, InvalidInputException, SQLException {

        BloodRequest req = requestDAO.getById(requestId);
        if (req == null) {
            throw new InvalidInputException("Blood request with ID " + requestId + " was not found.");
        }

        // Enforce ownership security boundary
        if (req.getHospitalId() != hospitalId) {
            throw new AccessDeniedException("Access denied: You are only authorized to cancel requests belonging to your own hospital (Hospital #" + hospitalId + ").");
        }

        // Only PENDING or MATCHED requests can be cancelled
        if (req.getStatus() != RequestStatus.PENDING && req.getStatus() != RequestStatus.MATCHED) {
            throw new InvalidInputException("Cannot cancel request #" + requestId + ": Only PENDING or MATCHED requests can be cancelled. Current status: " + req.getStatus());
        }

        return requestDAO.cancel(requestId);
    }
}
