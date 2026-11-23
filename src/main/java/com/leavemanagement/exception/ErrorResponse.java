package com.leavemanagement.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** The JSON shape returned for every error. fieldErrors is only filled for validation errors. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
