package com.bloodnetwork.exception;

/**
 * Custom exception thrown when login credentials fail verification.
 * For security reasons, the error message should never reveal whether
 * the username or password specifically was incorrect.
 */
public class AuthenticationException extends Exception {

    /**
     * Constructs an AuthenticationException with an error message.
     *
     * @param message Explanatory security message.
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Constructs an AuthenticationException with a message and cause.
     *
     * @param message Explanatory security message.
     * @param cause Underlying cause.
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
