package com.bloodnetwork.model;

/**
 * Represents a row in the 'app_user' table used for authentication and authorization.
 */
public class AppUser {

    private int userId;
    private String username;
    private String passwordHash;
    private String role; // 'ADMIN', 'DONOR', 'REQUESTER'

    public AppUser() {}

    public AppUser(int userId, String username, String passwordHash, String role) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "AppUser [ID: " + userId + ", Username: " + username + ", Role: " + role + "]";
    }
}
