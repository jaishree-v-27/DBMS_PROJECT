package com.bloodnetwork.util;

import com.bloodnetwork.exception.InvalidInputException;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class providing static methods to validate common input fields
 * across the application such as non-empty text, age, phone numbers, and blood groups.
 */
public class Validator {

    // List of accepted medical blood group types
    private static final List<String> VALID_BLOOD_GROUPS = Arrays.asList(
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    );

    // Private constructor to prevent instantiation of utility class
    private Validator() {}

    /**
     * Validates that a string is neither null nor blank.
     *
     * @param text The text value to validate.
     * @param fieldName The human-readable name of the field for the error message.
     * @throws InvalidInputException if text is null or empty after trimming.
     */
    public static void validateNonEmpty(String text, String fieldName) throws InvalidInputException {
        if (text == null || text.trim().isEmpty()) {
            throw new InvalidInputException(fieldName + " cannot be empty.");
        }
    }

    /**
     * Validates that a donor's age falls within the legally and medically accepted range (18 to 65).
     *
     * @param age The age in years.
     * @throws InvalidInputException if age is less than 18 or greater than 65.
     */
    public static void validateAge(int age) throws InvalidInputException {
        if (age < 18 || age > 65) {
            throw new InvalidInputException("Age must be between 18 and 65 years. Provided: " + age);
        }
    }

    /**
     * Validates that a phone number consists of exactly 10 digits.
     *
     * @param phone The phone string to validate.
     * @throws InvalidInputException if phone is null or does not match 10 consecutive digits.
     */
    public static void validatePhone(String phone) throws InvalidInputException {
        if (phone == null || !phone.trim().matches("^[0-9]{10}$")) {
            throw new InvalidInputException("Phone number must be exactly 10 digits (digits 0-9 only). Provided: " + phone);
        }
    }

    /**
     * Validates that a blood group matches one of the 8 standard blood group types:
     * A+, A-, B+, B-, AB+, AB-, O+, O-.
     *
     * @param group The blood group string (case-insensitive during check).
     * @throws InvalidInputException if group is null or not in the allowed list.
     */
    public static void validateBloodGroup(String group) throws InvalidInputException {
        if (group == null || !VALID_BLOOD_GROUPS.contains(group.trim().toUpperCase())) {
            throw new InvalidInputException("Invalid blood group '" + group + "'. Allowed values: " + VALID_BLOOD_GROUPS);
        }
    }
}
