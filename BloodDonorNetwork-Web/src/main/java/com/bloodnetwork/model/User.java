package com.bloodnetwork.model;

/**
 * Abstract base class representing a general user or actor in the system.
 * Contains common attributes shared by users and participants (e.g. name, phone, city).
 */
public abstract class User {

    private String name;
    private String phone;
    private String city;

    /**
     * Default no-arg constructor.
     */
    public User() {}

    /**
     * Parameterized constructor.
     *
     * @param name Full name of the user.
     * @param phone Contact phone number.
     * @param city City of residence/location.
     */
    public User(String name, String phone, String city) {
        this.name = name;
        this.phone = phone;
        this.city = city;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Abstract method to be implemented by subclasses returning their specific role.
     *
     * @return Role name as a string (e.g., "DONOR", "ADMIN", "REQUESTER")
     */
    public abstract String getRole();

    /**
     * Polymorphic method returning the list of accessible dashboard navigation
     * menus tailored for each specific user role.
     *
     * @return Array of dashboard menu tab names.
     */
    public abstract String[] getDashboardMenu();
}
