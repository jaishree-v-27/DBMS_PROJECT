package com.bloodnetwork.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Represents a blood donor in the system.
 * Inherits common contact and demographic fields from User.
 */
public class Donor extends User {

    private int donorId;
    private Integer userId; // Optional foreign key to app_user (can be null)
    private int age;
    private String gender;
    private String bloodGroup;
    private LocalDate lastDonationDate; // null if donor has never donated
    private boolean available;

    /**
     * Default constructor.
     */
    public Donor() {
        super();
        this.available = true;
    }

    /**
     * Constructor for creating a donor before persisting (without donorId).
     */
    public Donor(String name, int age, String gender, String bloodGroup,
                 String phone, String city, LocalDate lastDonationDate, boolean available) {
        super(name, phone, city);
        this.age = age;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.lastDonationDate = lastDonationDate;
        this.available = available;
    }

    /**
     * Constructor with donorId (e.g. when fetching from database).
     */
    public Donor(int donorId, Integer userId, String name, int age, String gender,
                 String bloodGroup, String phone, String city, LocalDate lastDonationDate, boolean available) {
        super(name, phone, city);
        this.donorId = donorId;
        this.userId = userId;
        this.age = age;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.lastDonationDate = lastDonationDate;
        this.available = available;
    }

    // Role implementation
    @Override
    public String getRole() {
        return "DONOR";
    }

    @Override
    public String[] getDashboardMenu() {
        return new String[] {"My Profile", "Eligibility", "Donation History", "Open Requests"};
    }

    /**
     * Determines whether the donor is currently eligible to donate blood.
     * 
     * Eligibility rules:
     * - Returns false if age is outside 18 to 65.
     * - Returns false if donor is marked as unavailable.
     * - Returns true if the donor has never donated before (lastDonationDate is null).
     * - Otherwise returns true only if at least 'gapDays' days have passed since the last donation.
     *
     * @param gapDays Minimum required interval between donations (e.g. 90 days).
     * @return true if eligible; false otherwise.
     */
    public boolean isEligible(int gapDays) {
        // Age must be between 18 and 65, and donor must be marked available
        if (age < 18 || age > 65 || !available) {
            return false;
        }

        // If donor has never donated, they are eligible
        if (lastDonationDate == null) {
            return true;
        }

        // Calculate days elapsed between last donation and current date
        long daysPassed = ChronoUnit.DAYS.between(lastDonationDate, LocalDate.now());
        return daysPassed >= gapDays;
    }

    // Getters and Setters
    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return String.format(
                "Donor [ID: %d | Name: %-15s | Blood Group: %-3s | Age: %2d | Gender: %s | Phone: %-10s | City: %-11s | Last Donation: %-10s | Available: %s]",
                donorId,
                getName(),
                bloodGroup,
                age,
                (gender != null ? gender : "-"),
                getPhone(),
                getCity(),
                (lastDonationDate != null ? lastDonationDate.toString() : "Never"),
                (available ? "YES" : "NO")
        );
    }
}
