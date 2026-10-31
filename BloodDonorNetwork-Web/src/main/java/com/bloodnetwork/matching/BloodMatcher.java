package com.bloodnetwork.matching;

import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.Donor;
import java.sql.SQLException;
import java.util.List;

/**
 * Strategy interface for finding eligible donors for a blood request.
 */
public interface BloodMatcher {

    /**
     * Finds and sorts eligible donors matching the given blood request.
     *
     * @param request The active blood request.
     * @return List of matching, eligible donors sorted by priority.
     * @throws SQLException if a database error occurs.
     */
    List<Donor> findMatches(BloodRequest request) throws SQLException;
}
