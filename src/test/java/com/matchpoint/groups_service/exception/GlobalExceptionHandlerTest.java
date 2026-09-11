package com.matchpoint.groups_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * The handler has no collaborators, so each @ExceptionHandler method is
 * exercised directly against a hand-built exception instance and the
 * resulting ProblemDetail is asserted (status, title, detail, and any
 * extra properties) — no Spring context / MockMvc needed.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpInputMessage httpInputMessage;

    @Mock
    private MethodParameter methodParameter;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        // no shared state; handler is stateless
    }

    @Test
    @DisplayName("ResourceNotFoundException maps to 404 with the exception message as detail")
    void handleResourceNotFound_shouldReturn404() {
        ResourceNotFoundException ex = ResourceNotFoundException.of("Group", 5L);

        ProblemDetail problem = handler.handleResourceNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource not found");
        assertThat(problem.getDetail()).isEqualTo("Group with id 5 not found");
    }

    @Test
    @DisplayName("BusinessRuleException maps to 409 with the exception message as detail")
    void handleBusinessRule_shouldReturn409() {
        BusinessRuleException ex = new BusinessRuleException("Group is already full");

        ProblemDetail problem = handler.handleBusinessRule(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Business rule violation");
        assertThat(problem.getDetail()).isEqualTo("Group is already full");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException maps to 400 and exposes field errors")
    void handleValidation_shouldReturn400WithFieldErrors() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "categoryRequest");
        bindingResult.addError(new FieldError("categoryRequest", "name", "must not be blank"));
        bindingResult.addError(new FieldError("categoryRequest", "playersPerTeam", "must not be null"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ProblemDetail problem = handler.handleValidation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation failed");
        assertThat(problem.getDetail()).isEqualTo("One or more fields are invalid");

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors)
                .containsEntry("name", "must not be blank")
                .containsEntry("playersPerTeam", "must not be null");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException maps to 400 with a generic client-facing message")
    void handleMalformedJson_shouldReturn400() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("JSON parse error: unexpected token", httpInputMessage);

        ProblemDetail problem = handler.handleMalformedJson(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Malformed request body");
        assertThat(problem.getDetail())
                .isEqualTo("The request body is malformed or contains an invalid value.");
    }

    @Test
    @DisplayName("Any unmapped Exception falls through to the catch-all and returns 500 without leaking details")
    void handleUnexpected_shouldReturn500AndNotLeakInternalMessage() {
        RuntimeException ex = new RuntimeException("Database connection pool exhausted");

        ProblemDetail problem = handler.handleUnexpected(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal server error");
        assertThat(problem.getDetail())
                .isEqualTo("An unexpected error occurred. Please try again later.")
                .doesNotContain("Database connection pool exhausted");
    }
}
