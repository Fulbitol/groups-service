package com.matchpoint.groups_service.exception;

/**
 * Thrown when a requested resource (Category, Group, JoinRequest, etc.)
 * does not exist. Handled by {@link GlobalExceptionHandler} and mapped
 * to an HTTP 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience factory for the common "entity + id" case, so services
     * don't have to hand-build the message every time.
     *
     * Example: ResourceNotFoundException.of("Category", categoryId)
     *          -> "Category with id 5 not found"
     */
    public static ResourceNotFoundException of(String entityName, Object id) {
        return new ResourceNotFoundException(entityName + " with id " + id + " not found");
    }
}