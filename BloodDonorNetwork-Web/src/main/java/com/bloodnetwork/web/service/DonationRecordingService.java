package com.bloodnetwork.web.service;

import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.exception.NotEligibleException;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Service managing whole blood donation recording.
 * Strictly verifies donor eligibility (18-65, available, 90-day clinical recovery).
 * The underlying MySQL trigger 'after_donation_insert' automatically updates
 * donor last_donation_date and increments hospital blood_stock.
 * Also evaluates and awards verified milestone achievements.
 */
@Service
public class DonationRecordingService {

    private final JdbcTemplate jdbcTemplate;
    private final DonorDAO donorDAO = new DonorDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final DonorAchievementService achievementService;

    @Autowired
    public DonationRecordingService(JdbcTemplate jdbcTemplate, DonorAchievementService achievementService) {
        this.jdbcTemplate = jdbcTemplate;
        this.achievementService = achievementService;
    }

    public record DonationResult(
            int donationId,
            String donorName,
            String hospitalName,
            String bloodGroup,
            int units,
            LocalDate donationDate,
            List<String> newlyAwardedBadges
    ) {}

    /**
     * Records a new blood donation session.
     */
    @Transactional(rollbackFor = Exception.class)
    public DonationResult recordDonation(int donorId, int hospitalId, int units, LocalDate donationDate)
            throws Exception {

        if (units <= 0 || units > 2) {
            throw new InvalidInputException("Donation units must be 1 or 2 whole blood units.");
        }
        if (donationDate == null) {
            donationDate = LocalDate.now();
        }
        if (donationDate.isAfter(LocalDate.now())) {
            throw new InvalidInputException("Donation date cannot be in the future.");
        }

        Donor donor = donorDAO.getById(donorId);
        if (donor == null) {
            throw new InvalidInputException("Donor #" + donorId + " was not found.");
        }

        Hospital hospital = hospitalDAO.getById(hospitalId);
        if (hospital == null) {
            throw new InvalidInputException("Hospital #" + hospitalId + " was not found.");
        }

        // Strict clinical eligibility check
        if (!donor.isEligible(90)) {
            String msg = "Donor " + donor.getName() + " is not currently eligible to donate. ";
            if (donor.getAge() < 18 || donor.getAge() > 65) {
                msg += "Age (" + donor.getAge() + ") falls outside the required 18 to 65 range.";
            } else if (!donor.isAvailable()) {
                msg += "Donor profile is marked as unavailable.";
            } else if (donor.getLastDonationDate() != null) {
                msg += "Mandatory 90-day recovery interval active (Last donated: " + donor.getLastDonationDate() + ").";
            }
            throw new NotEligibleException(msg);
        }

        // Insert into donation table
        // (Note: Trigger after_donation_insert automatically updates donor.last_donation_date and increments blood_stock)
        String sql = "INSERT INTO donation (donor_id, hospital_id, units, donation_date) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, donorId, hospitalId, units, Date.valueOf(donationDate));

        // Get generated donation ID
        Integer donationId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

        // Evaluate achievements
        List<String> newBadges = achievementService.evaluateAndAwardAchievements(donorId);

        return new DonationResult(
                donationId != null ? donationId : 0,
                donor.getName(),
                hospital.getName(),
                donor.getBloodGroup(),
                units,
                donationDate,
                newBadges
        );
    }
}
