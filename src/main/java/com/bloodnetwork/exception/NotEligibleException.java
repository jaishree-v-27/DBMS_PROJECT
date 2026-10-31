package com.bloodnetwork.exception;

/**
 * Custom exception thrown when a donor is medically or legally ineligible to donate blood
 * (e.g., outside age 18-65, marked unavailable, or has not completed the required 90-day waiting period).
 */
public class NotEligibleException extends Exception {

    /**
     * Constructs a NotEligibleException with a detailed reason.
     *
     * @param message Explanatory message detailing why the donor is ineligible.
     */
    public NotEligibleException(String message) {
        super(message);
    }

    /**
     * Constructs a NotEligibleException with a message and cause.
     *
     * @param message Explanatory message.
     * @param cause Underlying cause.
     */
    public NotEligibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
