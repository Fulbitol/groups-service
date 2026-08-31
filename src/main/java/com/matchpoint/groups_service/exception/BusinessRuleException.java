package com.matchpoint.groups_service.exception;

/**
 * Thrown when an operation is technically valid (the request is
 * well-formed and the referenced resources exist) but violates a
 * domain/business rule — e.g. trying to join a Group that is already
 * full. Handled by {@link GlobalExceptionHandler} and mapped to an
 * HTTP 409 Conflict response.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}