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
 * Strategy implementation that matches donors having the EXACT same blood group
 * as requested, located in the same city as the requesting hospital.
 */
public class ExactMatcher implements BloodMatcher {

    private final DonorDAO donorDAO;
    private final HospitalDAO hospitalDAO;

    public ExactMatcher() {
        this.donorDAO = new DonorDAO();
        this.hospitalDAO = new HospitalDAO();
    }

    public ExactMatcher(DonorDAO donorDAO, HospitalDAO hospitalDAO) {
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

        // 2. Fetch donors in the same city with the exact requested blood group
        List<String> targetGroup = Collections.singletonList(request.getBloodGroup());
        List<Donor> rawDonors = donorDAO.findByCityAndGroups(city, targetGroup);

        // 3. Keep ONLY medically eligible donors (age 18-65, available, 90 days gap)
        List<Donor> eligibleDonors = new ArrayList<>();
        for (Donor d : rawDonors) {
            if (d.isEligible(90)) {
                eligibleDonors.add(d);
            }
        }

        // 4. Sort: exact match first, never-donated first, then longest time since last donation
        eligibleDonors.sort(new DonorPriorityComparator(request.getBloodGroup()));

        return eligibleDonors;
    }
}
