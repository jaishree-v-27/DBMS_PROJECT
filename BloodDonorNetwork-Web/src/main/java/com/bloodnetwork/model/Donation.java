package com.bloodnetwork.model;

import java.time.LocalDate;

/**
 * Represents a blood donation event recorded at a hospital.
 */
public class Donation {

    private int donationId;
    private int donorId;
    private int hospitalId;
    private Integer requestId; // Optional link to blood_request (can be null for voluntary donations)
    private int units;
    private LocalDate donationDate;

    /**
     * Default constructor.
     */
    public Donation() {
        this.units = 1;
        this.donationDate = LocalDate.now();
    }

    /**
     * Constructor for creating a new donation before persisting (without donationId).
     */
    public Donation(int donorId, int hospitalId, Integer requestId, int units, LocalDate donationDate) {
        this.donorId = donorId;
        this.hospitalId = hospitalId;
        this.requestId = requestId;
        this.units = units;
        this.donationDate = donationDate;
    }

    /**
     * Full constructor for retrieving donation records from the database.
     */
    public Donation(int donationId, int donorId, int hospitalId, Integer requestId, int units, LocalDate donationDate) {
        this.donationId = donationId;
        this.donorId = donorId;
        this.hospitalId = hospitalId;
        this.requestId = requestId;
        this.units = units;
        this.donationDate = donationDate;
    }

    // Getters and Setters
    public int getDonationId() {
        return donationId;
    }

    public void setDonationId(int donationId) {
        this.donationId = donationId;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public int getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(int hospitalId) {
        this.hospitalId = hospitalId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public int getUnits() {
        return units;
    }

    public void setUnits(int units) {
        this.units = units;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
    }

    @Override
    public String toString() {
        return String.format(
                "Donation [ID: %d | Donor ID: %d | Hospital ID: %d | Request ID: %s | Units: %d | Date: %s]",
                donationId,
                donorId,
                hospitalId,
                (requestId != null ? String.valueOf(requestId) : "Voluntary"),
                units,
                (donationDate != null ? donationDate.toString() : "N/A")
        );
    }
}
