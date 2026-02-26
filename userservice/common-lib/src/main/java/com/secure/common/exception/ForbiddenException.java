package com.secure.common.exception;

/**
 * Exception thrown when a user is forbidden from accessing a resource.
 * Results in HTTP 403 Forbidden response.
 */
public class ForbiddenException extends RuntimeException {

    /**
     * Constructs a new ForbiddenException with the specified detail message.
     *
     * @param message the detail message
     */
    public ForbiddenException(String message) {
        super(message);
    }

    /**
     * Constructs a new ForbiddenException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
