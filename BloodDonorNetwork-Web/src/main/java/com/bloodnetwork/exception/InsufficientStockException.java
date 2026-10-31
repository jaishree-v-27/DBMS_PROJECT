package com.bloodnetwork.exception;

/**
 * Custom exception thrown during blood issuance when the requested hospital
 * does not have enough available units of the required blood group.
 */
public class InsufficientStockException extends Exception {

    /**
     * Constructs an InsufficientStockException with a detailed explanation.
     *
     * @param message Explanatory message (e.g. required units vs available units).
     */
    public InsufficientStockException(String message) {
        super(message);
    }

    /**
     * Constructs an InsufficientStockException with a message and cause.
     *
     * @param message Explanatory message.
     * @param cause Underlying cause.
     */
    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
