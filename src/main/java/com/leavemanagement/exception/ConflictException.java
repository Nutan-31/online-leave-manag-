package com.leavemanagement.exception;

/**
 * The request is valid, but the CURRENT STATE of the data does not allow it
 * (overlapping leave, insufficient balance, wrong leave status...)  ->  HTTP 409
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
