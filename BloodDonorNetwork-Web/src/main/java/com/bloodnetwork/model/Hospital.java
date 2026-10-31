package com.bloodnetwork.model;

/**
 * Represents a hospital or blood bank center in the system.
 */
public class Hospital {

    private int hospitalId;
    private Integer userId; // Optional foreign key to app_user (can be null)
    private String name;
    private String city;
    private String phone;

    /**
     * Default constructor.
     */
    public Hospital() {}

    /**
     * Constructor without hospitalId (used when inserting a new hospital).
     */
    public Hospital(String name, String city, String phone) {
        this.name = name;
        this.city = city;
        this.phone = phone;
    }

    /**
     * Parameterized constructor with all fields.
     */
    public Hospital(int hospitalId, Integer userId, String name, String city, String phone) {
        this.hospitalId = hospitalId;
        this.userId = userId;
        this.name = name;
        this.city = city;
        this.phone = phone;
    }

    // Getters and Setters
    public int getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(int hospitalId) {
        this.hospitalId = hospitalId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return String.format(
                "Hospital [ID: %d | Name: %-30s | City: %-12s | Phone: %-12s]",
                hospitalId,
                name,
                city,
                (phone != null ? phone : "N/A")
        );
    }
}
