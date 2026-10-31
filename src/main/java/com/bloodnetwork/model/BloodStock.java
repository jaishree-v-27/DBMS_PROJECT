package com.bloodnetwork.model;

/**
 * Represents the blood inventory quantity of a specific blood group at a hospital.
 */
public class BloodStock {

    private int hospitalId;
    private String bloodGroup;
    private int unitsAvailable;

    /**
     * Default constructor.
     */
    public BloodStock() {}

    /**
     * Parameterized constructor.
     *
     * @param hospitalId ID of the hospital holding the stock.
     * @param bloodGroup Blood group type (e.g. A+, O-).
     * @param unitsAvailable Number of units currently available.
     */
    public BloodStock(int hospitalId, String bloodGroup, int unitsAvailable) {
        this.hospitalId = hospitalId;
        this.bloodGroup = bloodGroup;
        this.unitsAvailable = unitsAvailable;
    }

    // Getters and Setters
    public int getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(int hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public int getUnitsAvailable() {
        return unitsAvailable;
    }

    public void setUnitsAvailable(int unitsAvailable) {
        this.unitsAvailable = unitsAvailable;
    }

    @Override
    public String toString() {
        return String.format(
                "BloodStock [Hospital ID: %d | Blood Group: %-3s | Units Available: %2d]",
                hospitalId,
                bloodGroup,
                unitsAvailable
        );
    }
}
