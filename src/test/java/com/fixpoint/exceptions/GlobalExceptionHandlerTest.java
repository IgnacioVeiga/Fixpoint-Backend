package com.fixpoint.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
    void handleGenericErrorShouldReturn500Response() {
        ResponseEntity<Map<String, Object>> response = handler.handleGenericError(new RuntimeException("Unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertBody(response.getBody(), 500, "Internal Server Error", "Unexpected server error");
    }

    private void assertBody(Map<String, Object> body, int status, String error, String message) {
        assertNotNull(body);
        assertEquals(status, body.get("status"));
        assertEquals(error, body.get("error"));
        assertEquals(message, body.get("message"));
        assertInstanceOf(OffsetDateTime.class, body.get("timestamp"));
    }
}
