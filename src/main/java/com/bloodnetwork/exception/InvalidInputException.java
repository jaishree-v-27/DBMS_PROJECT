package com.bloodnetwork.exception;

/**
 * Custom checked exception thrown when user input fails validation checks
 * (e.g., invalid age, incorrect phone number format, invalid blood group, etc.).
 */
public class InvalidInputException extends Exception {

    /**
     * Constructs a new InvalidInputException with a detailed error message.
     *
     * @param message Description of the validation failure.
     */
    public InvalidInputException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidInputException with a detailed message and cause.
     *
     * @param message Description of the validation failure.
     * @param cause Underlying exception that triggered this error.
     */
    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
