package com.bloodnetwork.web.service;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.matching.CompatibilityTable;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import com.bloodnetwork.model.RequestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service managing in-app transfusion notifications between hospitals and eligible voluntary donors.
 */
@Service
public class DonorNotificationService {

    private final JdbcTemplate jdbcTemplate;
    private final DonorDAO donorDAO = new DonorDAO();
    private final RequestDAO requestDAO = new RequestDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final RequestWorkflowService workflowService;

    @Autowired
    public DonorNotificationService(JdbcTemplate jdbcTemplate, RequestWorkflowService workflowService) {
        this.jdbcTemplate = jdbcTemplate;
        this.workflowService = workflowService;
    }

    public record NotificationItem(
            int notificationId,
            int donorId,
            int requestId,
            String hospitalName,
            String hospitalCity,
            String patientName,
            String bloodGroup,
            int unitsNeeded,
            String urgency,
            String status,
            LocalDateTime createdAt,
            LocalDateTime respondedAt
    ) {}

    public record DonorResponseSummary(
            int totalSent,
            int totalAccepted,
            int totalDeclined,
            int totalPending,
            List<Map<String, Object>> responses
    ) {}

    /**
     * Sends an in-app transfusion notification to a compatible voluntary donor.
     * Enforces eligibility: must be age 18-65, available=true, compatible blood group,
     * same city as hospital, and 90-day recovery compliant.
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean sendNotification(int requestId, int donorId, int actorUserId) throws Exception {
        BloodRequest request = requestDAO.getById(requestId);
        if (request == null) {
            throw new InvalidInputException("Blood request #" + requestId + " was not found.");
        }
        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.MATCHED) {
            throw new InvalidInputException("Cannot notify donors for a request with status: " + request.getStatus());
        }

        Donor donor = donorDAO.getById(donorId);
        if (donor == null) {
            throw new InvalidInputException("Donor #" + donorId + " was not found.");
        }

        Hospital hospital = hospitalDAO.getById(request.getHospitalId());
        if (hospital == null) {
            throw new InvalidInputException("Hospital profile was not found.");
        }

        // Clinical validation
        if (!donor.isEligible(90)) {
            throw new InvalidInputException("Donor " + donor.getName() + " is currently in 90-day recovery or does not meet clinical criteria.");
        }
        if (!donor.getCity().equalsIgnoreCase(hospital.getCity())) {
            throw new InvalidInputException("Geographical mismatch: Donor is in " + donor.getCity() + ", hospital is in " + hospital.getCity() + ".");
        }
        List<String> compatibleGroups = CompatibilityTable.donorGroupsFor(request.getBloodGroup());
        if (!compatibleGroups.contains(donor.getBloodGroup().trim().toUpperCase())) {
            throw new InvalidInputException("Hemolytic mismatch: Donor group " + donor.getBloodGroup() + " cannot donate to " + request.getBloodGroup() + ".");
        }

        // Insert notification (idempotent via UNIQUE KEY)
        String sql = "INSERT INTO donor_notification (donor_id, request_id, status, created_at) "
                   + "VALUES (?, ?, 'SENT', NOW()) "
                   + "ON DUPLICATE KEY UPDATE status = IF(status = 'EXPIRED', 'SENT', status)";
        int rows = jdbcTemplate.update(sql, donorId, requestId);

        // Record audit event
        workflowService.recordEvent(requestId, "DONOR_NOTIFIED", request.getStatus().name(), request.getStatus().name(),
                actorUserId, "In-app transfusion notification sent to voluntary donor " + donor.getName() 
                + " (" + donor.getBloodGroup() + ", " + donor.getPhone() + ").");

        return rows > 0;
    }

    /**
     * Responds to an in-app notification (Accept or Decline).
     */
    @Transactional(rollbackFor = Exception.class)
    public void respondToNotification(int notificationId, int donorId, String responseStatus) throws InvalidInputException {
        if (!"ACCEPTED".equalsIgnoreCase(responseStatus) && !"DECLINED".equalsIgnoreCase(responseStatus)) {
            throw new InvalidInputException("Invalid response status. Must be ACCEPTED or DECLINED.");
        }

        String sql = "SELECT n.notification_id, n.request_id, n.status, r.status AS req_status "
                   + "FROM donor_notification n "
                   + "JOIN blood_request r ON n.request_id = r.request_id "
                   + "WHERE n.notification_id = ? AND n.donor_id = ? FOR UPDATE";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, notificationId, donorId);
        if (rows.isEmpty()) {
            throw new InvalidInputException("Notification #" + notificationId + " not found or unauthorized.");
        }

        Map<String, Object> item = rows.get(0);
        int requestId = ((Number) item.get("request_id")).intValue();
        String currentReqStatus = (String) item.get("req_status");

        // Update notification
        jdbcTemplate.update(
                "UPDATE donor_notification SET status = ?, responded_at = NOW() WHERE notification_id = ?",
                responseStatus.toUpperCase(), notificationId
        );

        // If ACCEPTED, advance request to MATCHED if still PENDING
        if ("ACCEPTED".equalsIgnoreCase(responseStatus) && "PENDING".equalsIgnoreCase(currentReqStatus)) {
            jdbcTemplate.update("UPDATE blood_request SET status = 'MATCHED' WHERE request_id = ?", requestId);
            workflowService.recordEvent(requestId, "STATUS_UPDATE", "PENDING", "MATCHED", null,
                    "Request status updated to MATCHED following in-app acceptance by Donor #" + donorId + ".");
        }
    }

    /**
     * Fetches all in-app notifications received by a donor (inbox).
     */
    public List<NotificationItem> getNotificationsForDonor(int donorId) {
        String sql = "SELECT n.notification_id, n.donor_id, n.request_id, h.name AS hospital_name, h.city AS hospital_city, "
                   + "       r.patient_name, r.blood_group, r.units_needed, r.urgency, n.status, n.created_at, n.responded_at "
                   + "FROM donor_notification n "
                   + "JOIN blood_request r ON n.request_id = r.request_id "
                   + "JOIN hospital h ON r.hospital_id = h.hospital_id "
                   + "WHERE n.donor_id = ? "
                   + "ORDER BY n.created_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new NotificationItem(
                rs.getInt("notification_id"),
                rs.getInt("donor_id"),
                rs.getInt("request_id"),
                rs.getString("hospital_name"),
                rs.getString("hospital_city"),
                rs.getString("patient_name"),
                rs.getString("blood_group"),
                rs.getInt("units_needed"),
                rs.getString("urgency"),
                rs.getString("status"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getTimestamp("responded_at") != null ? rs.getTimestamp("responded_at").toLocalDateTime() : null
        ), donorId);
    }

    /**
     * Fetches notification breakdown and responses for a hospital request.
     */
    public DonorResponseSummary getResponseSummaryForRequest(int requestId) {
        String sql = "SELECT n.notification_id, n.donor_id, d.name AS donor_name, d.blood_group, d.phone, d.city, "
                   + "       n.status, n.created_at, n.responded_at "
                   + "FROM donor_notification n "
                   + "JOIN donor d ON n.donor_id = d.donor_id "
                   + "WHERE n.request_id = ? "
                   + "ORDER BY n.created_at ASC";

        List<Map<String, Object>> responses = jdbcTemplate.queryForList(sql, requestId);

        int sent = 0, accepted = 0, declined = 0, pending = 0;
        for (Map<String, Object> r : responses) {
            String st = (String) r.get("status");
            if ("ACCEPTED".equalsIgnoreCase(st)) accepted++;
            else if ("DECLINED".equalsIgnoreCase(st)) declined++;
            else if ("SENT".equalsIgnoreCase(st)) pending++;
            sent++;
        }

        return new DonorResponseSummary(sent, accepted, declined, pending, responses);
    }
}
