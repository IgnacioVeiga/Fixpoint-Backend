package com.fixpoint.exceptions;

import com.fixpoint.auth.exception.AuthenticationFailedException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundShouldReturn404Response() {
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(new EntityNotFoundException("Ticket not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertBody(response.getBody(), 404, "Not Found", "Ticket not found");
    }

    @Test
    void handleConflictShouldReturn409Response() {
        ResponseEntity<Map<String, Object>> response = handler.handleConflict(
                new DataIntegrityViolationException("Duplicate key")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertBody(response.getBody(), 409, "Conflict", "Duplicate key");
    }

    @Test
    void handleBadRequestShouldReturn400Response() {
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(
                new IllegalArgumentException("Invalid quantity")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertBody(response.getBody(), 400, "Bad Request", "Invalid quantity");
    }

    @Test
    void handleAuthenticationFailureShouldReturn401Response() {
        ResponseEntity<Map<String, Object>> response = handler.handleAuthenticationFailure(
                new AuthenticationFailedException("Invalid or expired session")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertBody(response.getBody(), 401, "Unauthorized", "Invalid or expired session");
    }

    @Test
    void handleGenericErrorShouldReturn500Response() {
        ResponseEntity<Map<String, Object>> response = handler.handleGenericError(new RuntimeException("Unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertBody(response.getBody(), 500, "Internal Server Error", "Unexpected server error");
    }

    @Test
    void handleNoResourceFoundShouldReturn404Response() {
        ResponseEntity<Map<String, Object>> response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.POST, "/api/v1/auth/register")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertBody(response.getBody(), 404, "Not Found", "Resource not found");
    }

    private void assertBody(Map<String, Object> body, int status, String error, String message) {
        assertNotNull(body);
        assertEquals(status, body.get("status"));
        assertEquals(error, body.get("error"));
        assertEquals(message, body.get("message"));
        assertInstanceOf(OffsetDateTime.class, body.get("timestamp"));
    }
}
