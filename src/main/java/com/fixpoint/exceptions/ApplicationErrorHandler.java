package com.fixpoint.exceptions;

public class ApplicationErrorHandler {
    private static final String ENV_VALIDATION_FAILURE_PREFIX = "Environment validation failed";

    public static void handle(Throwable ex) {
        if (hasEnvironmentValidationError(ex)) {
            System.err.println("\n--------------------------------------------------");
            System.err.println("ERROR: Required environment configuration is missing or invalid.");
            System.err.println("Review the details below and update your .env file or runtime variables:");
            System.err.println(extractRelevantMessage(ex));
            System.err.println("--------------------------------------------------\n");
            return;
        }

        if (hasDatabaseConnectionError(ex)) {
            System.err.println("\n--------------------------------------------------");
            System.err.println("ERROR: Could not connect to PostgreSQL.");
            System.err.println("Check DB_URL, DB_USERNAME and DB_PASSWORD values.");
            System.err.println("If you are using a cloud database, verify network access and SSL settings (for example '?sslmode=require').");
            System.err.println("--------------------------------------------------\n");
        }
        // More startup diagnostics can be added here.
    }

    private static boolean hasDatabaseConnectionError(Throwable ex) {
        while (ex != null) {
            String message = ex.getMessage();
            if (message != null && (message.contains("Connection to") || message.contains("FATAL: password authentication failed"))) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }

    private static boolean hasEnvironmentValidationError(Throwable ex) {
        return extractRelevantMessage(ex).startsWith(ENV_VALIDATION_FAILURE_PREFIX);
    }

    private static String extractRelevantMessage(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return "No additional details available.";
    }
}
