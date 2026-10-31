package com.bloodnetwork.matching;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Strategy implementation that matches donors across ALL compatible blood groups
 * based on medical transfusion compatibility rules, in the same city as the hospital.
 */
public class CompatibleMatcher implements BloodMatcher {

    private final DonorDAO donorDAO;
    private final HospitalDAO hospitalDAO;

    public CompatibleMatcher() {
        this.donorDAO = new DonorDAO();
        this.hospitalDAO = new HospitalDAO();
    }

    public CompatibleMatcher(DonorDAO donorDAO, HospitalDAO hospitalDAO) {
        this.donorDAO = donorDAO;
        this.hospitalDAO = hospitalDAO;
    }

    @Override
    public List<Donor> findMatches(BloodRequest request) throws SQLException {
        if (request == null) {
            return Collections.emptyList();
        }

        // 1. Look up the city of the requesting hospital
        Hospital hospital = hospitalDAO.getById(request.getHospitalId());
        if (hospital == null) {
            throw new SQLException("Cannot match donors: Hospital with ID " + request.getHospitalId() + " does not exist.");
        }
        String city = hospital.getCity();

        // 2. Look up all medically compatible blood groups for the patient
        List<String> compatibleGroups = CompatibilityTable.donorGroupsFor(request.getBloodGroup());
        if (compatibleGroups.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Fetch donors in that city belonging to any compatible group
        List<Donor> rawDonors = donorDAO.findByCityAndGroups(city, compatibleGroups);

        // 4. Keep ONLY medically eligible donors (age 18-65, available, 90 days gap)
        List<Donor> eligibleDonors = new ArrayList<>();
        for (Donor d : rawDonors) {
            if (d.isEligible(90)) {
                eligibleDonors.add(d);
            }
        }

        // 5. Sort: exact match first, then never-donated, then longest time since last donation
        eligibleDonors.sort(new DonorPriorityComparator(request.getBloodGroup()));

        return eligibleDonors;
    }
}
