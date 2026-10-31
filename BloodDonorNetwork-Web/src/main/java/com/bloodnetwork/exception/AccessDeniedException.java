package com.bloodnetwork.exception;

/**
 * Custom exception thrown when a user or requester attempts an operation
 * on a resource outside their authorized hospital or donor boundary.
 */
public class AccessDeniedException extends Exception {

    /**
     * Constructs an AccessDeniedException with a specific message.
     *
     * @param message Explanatory security/access denied message.
     */
    public AccessDeniedException(String message) {
        super(message);
    }

    /**
     * Constructs an AccessDeniedException with a message and cause.
     *
     * @param message Explanatory message.
     * @param cause Underlying cause.
     */
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
