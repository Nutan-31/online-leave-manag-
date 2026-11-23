package com.leavemanagement.exception;

/**
 * This person is not allowed to do this action (e.g. not the reporting manager)  ->  HTTP 403.
 * Real login/JWT security is added in Step 10; until then the "who" is passed as a request parameter.
 */
public class NotAuthorizedException extends RuntimeException {

    public NotAuthorizedException(String message) {
        super(message);
    }
}
