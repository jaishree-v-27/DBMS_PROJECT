package com.bloodnetwork.model;

/**
 * Represents an Administrator user with system-wide privileges to manage
 * donors, hospitals, blood requests, donations, stock, and reports.
 */
public class Admin extends User {

    private int userId;
    private String username;

    public Admin() {
        super();
    }

    public Admin(int userId, String username, String name, String phone, String city) {
        super(name, phone, city);
        this.userId = userId;
        this.username = username;
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

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String[] getDashboardMenu() {
        return new String[] {"Donors", "Hospitals", "Requests", "Donations", "Stock", "Reports"};
    }

    @Override
    public String toString() {
        return "Admin [User ID: " + userId + ", Username: " + username + ", Name: " + getName() + "]";
    }
}
