package com.bloodnetwork.web.service;

import com.bloodnetwork.exception.InsufficientStockException;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.matching.CompatibilityTable;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.RequestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service orchestrating the Blood Request state machine,
 * transactional blood issuance with SELECT ... FOR UPDATE row locking,
 * smart allocation proposals, and request_event audit logging.
 */
@Service
public class RequestWorkflowService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RequestWorkflowService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Represents a single audited transition event in the request lifecycle.
     */
    public record RequestEventItem(
            int eventId,
            int requestId,
            String eventType,
            String fromStatus,
            String toStatus,
            Integer actorUserId,
            String actorUsername,
            String note,
            LocalDateTime createdAt
    ) {}

    /**
     * Proposes a prioritized allocation plan for satisfying a blood request:
     * 1. Exact blood group match.
     * 2. Compatible non-universal groups.
     * 3. Universal donor O- conserved for last.
     */
    public record FulfilmentPlan(
            int requestId,
            String patientBloodGroup,
            int unitsNeeded,
            int exactAvailable,
            int compatibleAvailable,
            int totalAllocatable,
            int shortfall,
            List<AllocationUnit> plannedAllocations,
            boolean canFullyFulfill
    ) {}

    public record AllocationUnit(
            String bloodGroup,
            int unitsProposed,
            String clinicalReason
    ) {}

    /**
     * Validates whether a state machine transition is permitted.
     */
    public boolean isTransitionAllowed(RequestStatus from, RequestStatus to) {
        if (from == null || to == null) return false;
        if (from == to) return true;

        if (from == RequestStatus.PENDING) {
            return to == RequestStatus.MATCHED || to == RequestStatus.FULFILLED || to == RequestStatus.CANCELLED;
        } else if (from == RequestStatus.MATCHED) {
            return to == RequestStatus.FULFILLED || to == RequestStatus.CANCELLED || to == RequestStatus.PENDING;
        }
        // FULFILLED and CANCELLED are final and immutable
        return false;
    }

    /**
     * Records an audit event in request_event table.
     */
    public void recordEvent(int requestId, String eventType, String fromStatus, String toStatus, Integer actorUserId, String note) {
        String sql = "INSERT INTO request_event (request_id, event_type, from_status, to_status, actor_user_id, note) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, requestId, eventType, fromStatus, toStatus, actorUserId, note);
    }

    /**
     * Fetches all real audit events recorded for a given request.
     */
    public List<RequestEventItem> getEventsForRequest(int requestId) {
        String sql = "SELECT e.event_id, e.request_id, e.event_type, e.from_status, e.to_status, e.actor_user_id, "
                   + "       u.username AS actor_username, e.note, e.created_at "
                   + "FROM request_event e "
                   + "LEFT JOIN app_user u ON e.actor_user_id = u.user_id "
                   + "WHERE e.request_id = ? "
                   + "ORDER BY e.created_at ASC, e.event_id ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new RequestEventItem(
                rs.getInt("event_id"),
                rs.getInt("request_id"),
                rs.getString("event_type"),
                rs.getString("from_status"),
                rs.getString("to_status"),
                rs.getObject("actor_user_id") != null ? rs.getInt("actor_user_id") : null,
                rs.getString("actor_username"),
                rs.getString("note"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()
        ), requestId);
    }

    /**
     * Cancels an open blood request (status must be PENDING or MATCHED).
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelRequest(int requestId, int actorUserId, Integer hospitalIdScope, String reason) throws InvalidInputException {
        // Query current state
        String sql = "SELECT request_id, hospital_id, status FROM blood_request WHERE request_id = ? FOR UPDATE";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, requestId);
        if (rows.isEmpty()) {
            throw new InvalidInputException("Blood request #" + requestId + " does not exist.");
        }

        Map<String, Object> req = rows.get(0);
        int reqHospId = ((Number) req.get("hospital_id")).intValue();
        if (hospitalIdScope != null && hospitalIdScope != reqHospId) {
            throw new InvalidInputException("Access denied: You cannot cancel a demand belonging to another healthcare facility.");
        }

        String statusStr = (String) req.get("status");
        RequestStatus currentStatus = RequestStatus.valueOf(statusStr.toUpperCase());

        if (!isTransitionAllowed(currentStatus, RequestStatus.CANCELLED)) {
            throw new InvalidInputException("Cannot cancel request #" + requestId + " because it is currently " + currentStatus + ".");
        }

        // Update status
        jdbcTemplate.update("UPDATE blood_request SET status = 'CANCELLED' WHERE request_id = ?", requestId);

        // Record audit event
        recordEvent(requestId, "CANCELLED", currentStatus.name(), RequestStatus.CANCELLED.name(), actorUserId,
                (reason != null && !reason.isBlank()) ? reason : "Request cancelled by authorized user.");
    }

    /**
     * Issues blood units for a request atomically in a database transaction with row locking.
     * Guaranteed:
     * - No negative inventory.
     * - No double fulfilment.
     * - Exact row locking with SELECT ... FOR UPDATE.
     * - Audit log recorded on success.
     */
    @Transactional(rollbackFor = Exception.class)
    public void issueBloodAtomic(int requestId, Integer actorUserId, String actorRole)
            throws InvalidInputException, InsufficientStockException {

        // 1. Lock the request row
        String reqSql = "SELECT request_id, hospital_id, blood_group, units_needed, status, patient_name "
                      + "FROM blood_request WHERE request_id = ? FOR UPDATE";
        List<Map<String, Object>> reqRows = jdbcTemplate.queryForList(reqSql, requestId);
        if (reqRows.isEmpty()) {
            throw new InvalidInputException("Blood request #" + requestId + " was not found.");
        }

        Map<String, Object> req = reqRows.get(0);
        String currentStatusStr = (String) req.get("status");
        RequestStatus currentStatus = RequestStatus.valueOf(currentStatusStr.toUpperCase());
        int hospitalId = ((Number) req.get("hospital_id")).intValue();
        String bloodGroup = (String) req.get("blood_group");
        int unitsNeeded = ((Number) req.get("units_needed")).intValue();
        String patientName = (String) req.get("patient_name");

        if (!isTransitionAllowed(currentStatus, RequestStatus.FULFILLED)) {
            throw new InvalidInputException("Request #" + requestId + " is currently " + currentStatus 
                    + " and cannot be fulfilled.");
        }

        // 2. Lock stock row for this hospital and blood group with consistent lock order
        String stockSql = "SELECT units_available FROM blood_stock WHERE hospital_id = ? AND blood_group = ? FOR UPDATE";
        List<Map<String, Object>> stockRows = jdbcTemplate.queryForList(stockSql, hospitalId, bloodGroup);

        int currentUnits = stockRows.isEmpty() ? 0 : ((Number) stockRows.get(0).get("units_available")).intValue();

        if (currentUnits < unitsNeeded) {
            throw new InsufficientStockException("Insufficient stock for Hospital #" + hospitalId 
                    + " (Blood Group " + bloodGroup + "): Needed " + unitsNeeded + " units, but only " 
                    + currentUnits + " units available.");
        }

        // 3. Deduct stock ensuring it never drops below 0
        int updated = jdbcTemplate.update(
                "UPDATE blood_stock SET units_available = units_available - ? "
                + "WHERE hospital_id = ? AND blood_group = ? AND units_available >= ?",
                unitsNeeded, hospitalId, bloodGroup, unitsNeeded
        );

        if (updated == 0) {
            throw new InsufficientStockException("Concurrent deduction detected: stock was already allocated.");
        }

        // 4. Update request status to FULFILLED
        jdbcTemplate.update("UPDATE blood_request SET status = 'FULFILLED' WHERE request_id = ?", requestId);

        // 5. Write audit event
        String note = "Issued " + unitsNeeded + " units of " + bloodGroup + " for patient " + patientName 
                    + " from Hospital #" + hospitalId + " inventory. (Remaining stock: " + (currentUnits - unitsNeeded) + "u)";
        recordEvent(requestId, "FULFILLED", currentStatus.name(), RequestStatus.FULFILLED.name(), actorUserId, note);
    }

    /**
     * Generates a smart clinical allocation proposal for a patient request.
     */
    public FulfilmentPlan computeSmartAllocationPlan(int hospitalId, int requestId, String patientGroup, int unitsNeeded) {
        // Query hospital stock for all compatible blood groups
        List<String> compatibleGroups = CompatibilityTable.donorGroupsFor(patientGroup);

        Map<String, Integer> availableMap = new LinkedHashMap<>();
        for (String g : compatibleGroups) {
            availableMap.put(g, 0);
        }

        String sql = "SELECT blood_group, units_available FROM blood_stock WHERE hospital_id = ?";
        jdbcTemplate.query(sql, (rs) -> {
            String bg = rs.getString("blood_group");
            int u = rs.getInt("units_available");
            if (availableMap.containsKey(bg)) {
                availableMap.put(bg, u);
            }
        }, hospitalId);

        int exactAvailable = availableMap.getOrDefault(patientGroup, 0);
        int totalCompatible = 0;
        for (int u : availableMap.values()) {
            totalCompatible += u;
        }

        List<AllocationUnit> planned = new ArrayList<>();
        int remainingNeeded = unitsNeeded;

        // Step 1: Use exact match first
        if (exactAvailable > 0 && remainingNeeded > 0) {
            int takeExact = Math.min(exactAvailable, remainingNeeded);
            planned.add(new AllocationUnit(patientGroup, takeExact, "Exact match: primary clinical choice"));
            remainingNeeded -= takeExact;
        }

        // Step 2: Use other compatible non-O- groups next
        for (String g : compatibleGroups) {
            if (remainingNeeded <= 0) break;
            if (g.equals(patientGroup) || g.equals("O-")) continue;

            int avail = availableMap.getOrDefault(g, 0);
            if (avail > 0) {
                int take = Math.min(avail, remainingNeeded);
                planned.add(new AllocationUnit(g, take, "Compatible match: antigen-safe secondary choice"));
                remainingNeeded -= take;
            }
        }

        // Step 3: Use universal donor O- last (to conserve for emergency O- patients)
        if (remainingNeeded > 0 && !patientGroup.equals("O-")) {
            int oNegAvail = availableMap.getOrDefault("O-", 0);
            if (oNegAvail > 0) {
                int take = Math.min(oNegAvail, remainingNeeded);
                planned.add(new AllocationUnit("O-", take, "Universal donor O- reserve used last"));
                remainingNeeded -= take;
            }
        }

        int totalAllocatable = unitsNeeded - remainingNeeded;
        int shortfall = Math.max(0, remainingNeeded);

        return new FulfilmentPlan(
                requestId,
                patientGroup,
                unitsNeeded,
                exactAvailable,
                totalCompatible,
                totalAllocatable,
                shortfall,
                planned,
                shortfall == 0
        );
    }
}
