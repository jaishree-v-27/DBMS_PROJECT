package com.bloodnetwork.exception;

/**
 * Custom exception thrown when no eligible donors are found matching a blood request.
 * Carries a clear, descriptive message to inform users about the search criteria and outcome.
 */
public class NoMatchFoundException extends Exception {

    /**
     * Constructs a NoMatchFoundException with a detailed explanation.
     *
     * @param message Explanatory message (e.g. details of blood group and city searched).
     */
    public NoMatchFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a NoMatchFoundException with a message and an underlying cause.
     *
     * @param message Explanatory message.
     * @param cause Underlying cause.
     */
    public NoMatchFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
