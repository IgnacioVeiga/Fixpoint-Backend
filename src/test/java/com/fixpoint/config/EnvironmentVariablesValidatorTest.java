package com.fixpoint.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentVariablesValidatorTest {

    @Test
    void shouldSkipValidationForMockProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("mock");

        EnvironmentVariablesValidator validator = new EnvironmentVariablesValidator(environment);

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void shouldFailWhenRequiredVariablesAreMissingInDev() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        EnvironmentVariablesValidator validator = new EnvironmentVariablesValidator(environment);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments(new String[0]))
        );

        assertTrue(ex.getMessage().contains("Missing environment variable 'DB_URL'"));
        assertTrue(ex.getMessage().contains("Missing environment variable 'DB_USERNAME'"));
        assertTrue(ex.getMessage().contains("Missing environment variable 'DB_PASSWORD'"));
        assertTrue(ex.getMessage().contains("Missing environment variable 'JWT_SECRET'"));
    }

    @Test
    void shouldFailWhenJwtSecretIsTooShort() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        environment.setProperty("DB_URL", "jdbc:postgresql://cloud-db:5432/fixpoint?sslmode=require");
        environment.setProperty("DB_USERNAME", "user");
        environment.setProperty("DB_PASSWORD", "password");
        environment.setProperty("JWT_SECRET", "short-secret");

        EnvironmentVariablesValidator validator = new EnvironmentVariablesValidator(environment);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments(new String[0]))
        );

        assertTrue(ex.getMessage().contains("JWT_SECRET must have at least 32 characters."));
    }

    @Test
    void shouldPassWhenRequiredVariablesArePresent() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        environment.setProperty("DB_URL", "jdbc:postgresql://cloud-db:5432/fixpoint?sslmode=require");
        environment.setProperty("DB_USERNAME", "cloud_user");
        environment.setProperty("DB_PASSWORD", "cloud_password");
        environment.setProperty("JWT_SECRET", "this-is-a-valid-jwt-secret-with-min-32");
        environment.setProperty("JWT_EXPIRATION_SECONDS", "43200");

        EnvironmentVariablesValidator validator = new EnvironmentVariablesValidator(environment);

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments(new String[0])));
    }
}
