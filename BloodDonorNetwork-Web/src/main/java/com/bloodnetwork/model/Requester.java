package com.bloodnetwork.model;

/**
 * Represents a Hospital Requester user authorized to submit and manage blood requests.
 */
public class Requester extends User {

    private int userId;
    private String username;
    private Integer hospitalId; // Linked hospital ID

    public Requester() {
        super();
    }

    public Requester(int userId, String username, String name, String phone, String city, Integer hospitalId) {
        super(name, phone, city);
        this.userId = userId;
        this.username = username;
        this.hospitalId = hospitalId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Integer hospitalId) {
        this.hospitalId = hospitalId;
    }

    @Override
    public String getRole() {
        return "REQUESTER";
    }

    @Override
    public String[] getDashboardMenu() {
        return new String[] {"Raise Request", "My Requests", "Find Donors", "Our Stock"};
    }

    @Override
    public String toString() {
        return "Requester [User ID: " + userId + ", Username: " + username + ", Hospital ID: " + hospitalId + "]";
    }
}
