package com.matchpoint.groups_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handling for all REST controllers.
 *
 * Uses Spring's {@link ProblemDetail} (RFC 7807 "Problem Details for
 * HTTP APIs") so every error response follows the same, self-describing
 * shape: type, title, status, detail, and — where relevant — extra
 * properties like field-level validation errors.
 *
 * Order of handlers below goes from most specific to most generic,
 * mirroring how they should be read/maintained.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * A requested resource (Category, Group, JoinRequest, etc.) does not exist.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        return problem;
    }

    /**
     * The request is well-formed and the resources exist, but the
     * operation violates a domain rule (e.g. joining a full Group).
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        log.debug("Business rule violation: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Business rule violation");
        return problem;
    }

    /**
     * Bean Validation (@Valid) failures on request DTOs, e.g. a blank
     * Category name or an empty positionCounts map. Returns which
     * field(s) failed and why, instead of a generic 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.debug("Validation failed: {}", fieldErrors);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more fields are invalid"
        );
        problem.setTitle("Validation failed");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /**
     * Malformed request body: invalid JSON syntax, or a value that
     * doesn't match the expected type (e.g. an invalid enum value as
     * a Map key, like sending "additionalProp1" instead of a real
     * Position such as "ARQUERO"). This is always a client error, so
     * it must map to 400 — without this handler it would fall through
     * to the generic 500 catch-all below, which is misleading.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex) {
        log.debug("Malformed request body: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The request body is malformed or contains an invalid value."
        );
        problem.setTitle("Malformed request body");
        return problem;
    }

    /**
     * Catch-all safety net. Never leak the raw exception/stacktrace to
     * the client — log the full detail server-side and return a generic
     * message instead.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        problem.setTitle("Internal server error");
        return problem;
    }
}