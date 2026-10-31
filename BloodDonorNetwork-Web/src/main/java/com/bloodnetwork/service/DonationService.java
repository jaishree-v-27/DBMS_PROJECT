package com.bloodnetwork.service;

import com.bloodnetwork.dao.DonationDAO;
import com.bloodnetwork.dao.DonorDAO;
import com.bloodnetwork.dao.HospitalDAO;
import com.bloodnetwork.dao.RequestDAO;
import com.bloodnetwork.exception.InvalidInputException;
import com.bloodnetwork.exception.NotEligibleException;
import com.bloodnetwork.model.BloodRequest;
import com.bloodnetwork.model.Donation;
import com.bloodnetwork.model.Donor;
import com.bloodnetwork.model.Hospital;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Service class handling business logic and validations for blood donations.
 */
public class DonationService {

    private final DonationDAO donationDAO;
    private final DonorDAO donorDAO;
    private final HospitalDAO hospitalDAO;
    private final RequestDAO requestDAO;

    public DonationService() {
        this.donationDAO = new DonationDAO();
        this.donorDAO = new DonorDAO();
        this.hospitalDAO = new HospitalDAO();
        this.requestDAO = new RequestDAO();
    }

    public DonationService(DonationDAO donationDAO, DonorDAO donorDAO, HospitalDAO hospitalDAO, RequestDAO requestDAO) {
        this.donationDAO = donationDAO;
        this.donorDAO = donorDAO;
        this.hospitalDAO = hospitalDAO;
        this.requestDAO = requestDAO;
    }

    /**
     * Validates donor eligibility and records a new donation.
     * Database triggers automatically update the donor's last donation date and inventory stock.
     *
     * @param donorId The donor ID.
     * @param hospitalId The hospital ID where donation occurs.
     * @param requestIdOrNull Optional linked blood request ID (can be null).
     * @param units Number of units donated (> 0).
     * @param date Date of donation (cannot be in the future).
     * @return Generated donation ID.
     * @throws InvalidInputException if parameters fail basic validation.
     * @throws NotEligibleException if the donor fails the 90-day or demographic criteria.
     * @throws SQLException if a database error occurs.
     */
    public int recordDonation(int donorId, int hospitalId, Integer requestIdOrNull, int units, LocalDate date)
            throws InvalidInputException, NotEligibleException, SQLException {

        // 1. Basic input validation
        if (units <= 0) {
            throw new InvalidInputException("Donation units must be greater than 0. Provided: " + units);
        }
        if (date == null) {
            throw new InvalidInputException("Donation date cannot be null.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidInputException("Donation date cannot be in the future. Provided: " + date);
        }

        // 2. Validate hospital existence
        Hospital hospital = hospitalDAO.getById(hospitalId);
        if (hospital == null) {
            throw new InvalidInputException("Hospital with ID " + hospitalId + " does not exist.");
        }

        // 3. Validate donor existence
        Donor donor = donorDAO.getById(donorId);
        if (donor == null) {
            throw new InvalidInputException("Donor with ID " + donorId + " does not exist.");
        }

        // 4. Validate donor eligibility using the 90-day medical rule
        if (!donor.isEligible(90)) {
            if (donor.getAge() < 18 || donor.getAge() > 65) {
                throw new NotEligibleException("Donor #" + donorId + " (" + donor.getName() + ") is not eligible: Age "
                        + donor.getAge() + " is outside the permissible donation age of 18 to 65.");
            }
            if (!donor.isAvailable()) {
                throw new NotEligibleException("Donor #" + donorId + " (" + donor.getName()
                        + ") is not eligible: Donor account is currently marked as unavailable.");
            }
            if (donor.getLastDonationDate() != null) {
                long daysPassed = ChronoUnit.DAYS.between(donor.getLastDonationDate(), LocalDate.now());
                long daysLeft = 90 - daysPassed;
                if (daysLeft > 0) {
                    throw new NotEligibleException("Donor #" + donorId + " (" + donor.getName()
                            + ") is not eligible: Donated " + daysPassed + " days ago on " + donor.getLastDonationDate()
                            + ". Must wait " + daysLeft + " more day(s) before donating again (90 days required).");
                }
            }
            throw new NotEligibleException("Donor #" + donorId + " (" + donor.getName() + ") is not eligible to donate.");
        }

        // 5. If linked to a blood request, verify request exists
        if (requestIdOrNull != null && requestIdOrNull > 0) {
            BloodRequest request = requestDAO.getById(requestIdOrNull);
            if (request == null) {
                throw new InvalidInputException("Blood request with ID " + requestIdOrNull + " does not exist.");
            }
        } else {
            requestIdOrNull = null;
        }

        // 6. Record the donation in the database (Trigger handles stock & donor date updates)
        Donation donation = new Donation(donorId, hospitalId, requestIdOrNull, units, date);
        return donationDAO.add(donation);
    }
}
