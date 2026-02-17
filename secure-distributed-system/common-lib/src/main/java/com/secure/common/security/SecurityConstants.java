package com.secure.common.security;

/**
 * Security constants used across the application.
 * Contains JWT token configuration and security-related constants.
 */
public interface SecurityConstants {

    /**
     * HTTP header name for authorization token
     */
    String TOKEN_HEADER = "Authorization";

    /**
     * Prefix for the bearer token
     */
    String TOKEN_PREFIX = "Bearer ";

    /**
     * Type of token used
     */
    String TOKEN_TYPE = "JWT";

    /**
     * Token expiration time in milliseconds (15 minutes)
     */
    long TOKEN_EXPIRATION_TIME = 900000L;

    /**
     * Refresh token expiration time in milliseconds (7 days)
     */
    long REFRESH_TOKEN_EXPIRATION_TIME = 604800000L;
}
