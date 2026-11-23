package com.leavemanagement.exception;

/** The request itself breaks a business rule (invalid manager, wrong department, bad dates...)  ->  HTTP 400 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
