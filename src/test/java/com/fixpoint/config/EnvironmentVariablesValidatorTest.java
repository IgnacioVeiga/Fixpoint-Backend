package com.fixpoint.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentVariablesValidatorTest {

    @Test
    void shouldSkipValidationForNonExternalDbProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

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
        environment.setProperty("JWT_ACCESS_EXPIRATION_SECONDS", "900");
        environment.setProperty("AUTH_REFRESH_EXPIRATION_SECONDS", "43200");
        environment.setProperty("AUTH_REFRESH_REMEMBER_EXPIRATION_SECONDS", "2592000");
        environment.setProperty("security.auth.refresh.cookie.same-site", "Lax");
        environment.setProperty("security.auth.refresh.cookie.secure", "false");

        EnvironmentVariablesValidator validator = new EnvironmentVariablesValidator(environment);

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void shouldFailWhenRefreshRememberDurationIsLowerThanDefaultRefreshDuration() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        environment.setProperty("DB_URL", "jdbc:postgresql://cloud-db:5432/fixpoint?sslmode=require");
        environment.setProperty("DB_USERNAME", "cloud_user");
        environment.setProperty("DB_PASSWORD", "cloud_password");
        environment.setProperty("JWT_SECRET", "this-is-a-valid-jwt-secret-with-min-32");
        environment.setProperty("AUTH_REFRESH_EXPIRATION_SECONDS", "86400");
        environment.setProperty("AUTH_REFRESH_REMEMBER_EXPIRATION_SECONDS", "43200");

        EnvironmentVariablesValidator validator = new EnvironmentVariablesValidator(environment);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments(new String[0]))
        );

        assertTrue(ex.getMessage().contains("AUTH_REFRESH_REMEMBER_EXPIRATION_SECONDS must be greater than or equal"));
    }

    @Test
    void shouldFailInProdWhenSameSiteNoneAndSecureCookieIsDisabled() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("DB_URL", "jdbc:postgresql://cloud-db:5432/fixpoint?sslmode=require");
        environment.setProperty("DB_USERNAME", "cloud_user");
        environment.setProperty("DB_PASSWORD", "cloud_password");
        environment.setProperty("JWT_SECRET", "this-is-a-valid-jwt-secret-with-min-32");
        environment.setProperty("security.auth.refresh.cookie.same-site", "None");
        environment.setProperty("security.auth.refresh.cookie.secure", "false");

        EnvironmentVariablesValidator validator = new EnvironmentVariablesValidator(environment);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments(new String[0]))
        );

        assertTrue(ex.getMessage().contains("security.auth.refresh.cookie.secure must be true when SameSite=None."));
    }
}
