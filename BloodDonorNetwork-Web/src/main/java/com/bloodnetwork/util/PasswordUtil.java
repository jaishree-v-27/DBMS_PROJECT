package com.bloodnetwork.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class providing SHA-256 password hashing and verification.
 */
public class PasswordUtil {

    private PasswordUtil() {}

    /**
     * Hashes a plain-text password using SHA-256 and returns a lowercase hexadecimal string.
     *
     * @param password The plain-text password to hash.
     * @return 64-character lowercase hex string representing the SHA-256 digest.
     */
    public static String hash(String password) {
        if (password == null) {
            password = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in this runtime environment.", e);
        }
    }

    /**
     * Verifies whether a plain-text password matches a stored SHA-256 hash.
     *
     * @param plain The plain-text password provided by user.
     * @param storedHash The stored hash from the database.
     * @return true if matches, false otherwise.
     */
    public static boolean verify(String plain, String storedHash) {
        if (plain == null || storedHash == null) {
            return false;
        }
        String computedHash = hash(plain);
        return computedHash.equalsIgnoreCase(storedHash.trim());
    }
}
