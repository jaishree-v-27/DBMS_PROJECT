package com.bloodnetwork.util;

import com.bloodnetwork.model.User;

/**
 * Global session manager storing authentication details of the currently logged-in user.
 */
public class Session {

    private static Session currentSession;

    private final int userId;
    private final String username;
    private final String role;
    private final Integer donorId;
    private final Integer hospitalId;
    private final User user;

    private Session(int userId, String username, String role, Integer donorId, Integer hospitalId, User user) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.donorId = donorId;
        this.hospitalId = hospitalId;
        this.user = user;
    }

    /**
     * Initializes the user session upon successful login.
     */
    public static synchronized void start(int userId, String username, String role, Integer donorId, Integer hospitalId, User user) {
        currentSession = new Session(userId, username, role, donorId, hospitalId, user);
    }

    /**
     * Retrieves the active session, or null if no user is currently logged in.
     */
    public static synchronized Session get() {
        return currentSession;
    }

    /**
     * Checks if a user is currently logged in.
     */
    public static synchronized boolean isLoggedIn() {
        return currentSession != null;
    }

    /**
     * Clears the current user session.
     */
    public static synchronized void logout() {
        currentSession = null;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public Integer getDonorId() {
        return donorId;
    }

    public Integer getHospitalId() {
        return hospitalId;
    }

    public User getUser() {
        return user;
    }
}
