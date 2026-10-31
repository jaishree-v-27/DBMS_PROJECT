package com.bloodnetwork.web.service;

import com.bloodnetwork.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing authentication verification, BCrypt password upgrades,
 * and brute-force attempt throttling (5 attempts with 10-minute lockout).
 */
@Service
public class SecurityService {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // In-memory brute-force lock tracking
    private final Map<String, LockoutEntry> failedAttempts = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 10 * 60 * 1000L; // 10 minutes

    @Autowired
    public SecurityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class LockoutEntry {
        int attempts;
        long lockedUntil;

        LockoutEntry(int attempts, long lockedUntil) {
            this.attempts = attempts;
            this.lockedUntil = lockedUntil;
        }
    }

    /**
     * Checks if the username is currently locked out due to excessive failed attempts.
     */
    public boolean isLockedOut(String username) {
        if (username == null) return false;
        LockoutEntry entry = failedAttempts.get(username.trim().toLowerCase());
        if (entry != null && entry.lockedUntil > System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    /**
     * Gets minutes remaining for a locked-out account.
     */
    public long getRemainingLockoutMinutes(String username) {
        if (username == null) return 0;
        LockoutEntry entry = failedAttempts.get(username.trim().toLowerCase());
        if (entry != null && entry.lockedUntil > System.currentTimeMillis()) {
            long remainingMs = entry.lockedUntil - System.currentTimeMillis();
            return (remainingMs / 60000L) + 1;
        }
        return 0;
    }

    /**
     * Records a failed login attempt for username.
     */
    public void recordFailedAttempt(String username) {
        if (username == null) return;
        String key = username.trim().toLowerCase();
        failedAttempts.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now > existing.lockedUntil) {
                return new LockoutEntry(1, 0);
            }
            int newCount = existing.attempts + 1;
            long lockUntil = (newCount >= MAX_FAILED_ATTEMPTS) ? now + LOCKOUT_DURATION_MS : 0;
            return new LockoutEntry(newCount, lockUntil);
        });
    }

    /**
     * Resets failed attempt counter on successful login.
     */
    public void recordSuccessfulLogin(String username) {
        if (username != null) {
            failedAttempts.remove(username.trim().toLowerCase());
        }
    }

    /**
     * Verifies raw password against stored hash.
     * Supports BCrypt hashes as well as legacy SHA-256 hashes.
     * When a legacy SHA-256 password matches, it is upgraded transparently to BCrypt in MySQL.
     */
    public boolean verifyAndUpgradePassword(int userId, String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }

        // 1. Check if hash is already BCrypt ($2a$, $2b$, or $2y$)
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedHash);
        }

        // 2. Otherwise verify using legacy SHA-256
        boolean matches = PasswordUtil.verify(rawPassword, storedHash);
        if (matches) {
            // Transparently upgrade to BCrypt
            String newBcryptHash = passwordEncoder.encode(rawPassword);
            try {
                jdbcTemplate.update("UPDATE app_user SET password_hash = ? WHERE user_id = ?", newBcryptHash, userId);
            } catch (Exception ignored) {
                // Keep login working even if update is temporarily delayed
            }
            return true;
        }

        return false;
    }

    /**
     * Changes user password after verifying current password.
     * New password is saved as a secure BCrypt hash.
     *
     * @param userId User ID
     * @param currentPassword Current plain-text password
     * @param newPassword New plain-text password
     * @return true if password was changed successfully, false if current password was incorrect
     */
    public boolean changePassword(int userId, String currentPassword, String newPassword) {
        if (currentPassword == null || newPassword == null || newPassword.trim().length() < 4) {
            return false;
        }

        String sql = "SELECT password_hash FROM app_user WHERE user_id = ?";
        var rows = jdbcTemplate.queryForList(sql, userId);
        if (rows.isEmpty()) {
            return false;
        }

        String storedHash = (String) rows.get(0).get("password_hash");
        boolean valid = verifyAndUpgradePassword(userId, currentPassword, storedHash);
        if (!valid) {
            return false;
        }

        String newBcryptHash = passwordEncoder.encode(newPassword.trim());
        jdbcTemplate.update("UPDATE app_user SET password_hash = ? WHERE user_id = ?", newBcryptHash, userId);
        return true;
    }
}
