package com.ecabs.ridematching.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Centralized exception handler - maps domain exceptions to appropriate HTTP status codes and RFC 7807 ProblemDetail
 * responses.
 *
 * <p>Using ProblemDetail (Spring 6 / Boot 3) is idiomatic and gives a consistent structure for error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoDriverAvailableException.class)
    public ProblemDetail handleNoDriverAvailable(NoDriverAvailableException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        detail.setType(URI.create("/errors/no-driver-available"));
        detail.setTitle("No Driver Available");
        return detail;
    }

    @ExceptionHandler(DriverNotFoundException.class)
    public ProblemDetail handleDriverNotFound(DriverNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setType(URI.create("/errors/driver-not-found"));
        detail.setTitle("Driver Not Found");
        return detail;
    }

    @ExceptionHandler(RideNotFoundException.class)
    public ProblemDetail handleRideNotFound(RideNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setType(URI.create("/errors/ride-not-found"));
        detail.setTitle("Ride Not Found");
        return detail;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setType(URI.create("/errors/invalid-state"));
        detail.setTitle("Invalid state transition");
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("Validation failed");
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setType(URI.create("/errors/validation-error"));
        detail.setTitle("Validation Error");
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .findFirst()
                .orElse("Constraint violation");
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setType(URI.create("/errors/validation"));
        detail.setTitle("Validation Error");
        return detail;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + "' is missing");
        detail.setType(URI.create("/errors/validation"));
        detail.setTitle("Validation Error");
        return detail;
    }
}
