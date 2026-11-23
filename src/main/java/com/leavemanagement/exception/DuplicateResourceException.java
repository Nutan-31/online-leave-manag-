package com.leavemanagement.exception;

/** Unique value already used (email, username, department name...)  ->  HTTP 409 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
