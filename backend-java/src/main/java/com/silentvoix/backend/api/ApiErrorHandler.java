package com.silentvoix.backend.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns refusals into {@code {"error": "<code>"}} bodies with the matching status. */
@RestControllerAdvice
class ApiErrorHandler {

    /** Body of every error response. */
    record ApiError(String error) {
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> refused(ApiException e) {
        return ResponseEntity.status(e.status()).body(new ApiError(e.code()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError("bad_request"));
    }
}
