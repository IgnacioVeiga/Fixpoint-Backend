package com.fixpoint.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.env.validation.enabled", havingValue = "true", matchIfMissing = true)
public class EnvironmentVariablesValidator implements ApplicationRunner {

    private static final Set<String> PROFILES_WITH_EXTERNAL_DB = Set.of("dev", "qa", "prod");
    private static final String ENV_VALIDATION_FAILURE_PREFIX = "Environment validation failed";

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        Set<String> activeProfiles = resolveActiveProfiles();
        if (activeProfiles.stream().noneMatch(PROFILES_WITH_EXTERNAL_DB::contains)) {
            return;
        }

        List<String> errors = new ArrayList<>();
        String dbUrl = requireEnvironmentVariable(
                "DB_URL",
                errors,
                "Example: jdbc:postgresql://db-host:5432/fixpoint?sslmode=require"
        );
        requireEnvironmentVariable("DB_USERNAME", errors, "Example: postgres");
        requireEnvironmentVariable("DB_PASSWORD", errors, "Example: secure-password");
        String jwtSecret = requireEnvironmentVariable(
                "JWT_SECRET",
                errors,
                "At least 32 characters"
        );
        validateJwtExpiration(errors);
        validateDbUrl(dbUrl, errors);
        validateJwtSecret(jwtSecret, errors);

        if (!errors.isEmpty()) {
            String message = buildValidationMessage(activeProfiles, errors);
            log.error(message);
            throw new IllegalStateException(message);
        }

        log.info("Environment validation passed for profiles {}", activeProfiles);
    }

    private Set<String> resolveActiveProfiles() {
        Set<String> profiles = new LinkedHashSet<>(Arrays.asList(environment.getActiveProfiles()));
        if (profiles.isEmpty()) {
            profiles.addAll(Arrays.asList(environment.getDefaultProfiles()));
        }
        return profiles;
    }

    private String requireEnvironmentVariable(String variableName, List<String> errors, String hint) {
        String value = environment.getProperty(variableName);
        if (!StringUtils.hasText(value)) {
            errors.add("Missing environment variable '" + variableName + "'. " + hint);
            return "";
        }
        return value.trim();
    }

    private void validateDbUrl(String dbUrl, List<String> errors) {
        if (!StringUtils.hasText(dbUrl)) {
            return;
        }

        if (!dbUrl.startsWith("jdbc:postgresql://")) {
            errors.add("Invalid DB_URL format. Expected it to start with 'jdbc:postgresql://'.");
            return;
        }

        boolean cloudHost = !dbUrl.contains("localhost") && !dbUrl.contains("127.0.0.1");
        boolean containsSslMode = dbUrl.toLowerCase().contains("sslmode=");
        if (cloudHost && !containsSslMode) {
            log.warn("DB_URL points to a non-local host and does not define 'sslmode'. " +
                    "Some cloud providers require '?sslmode=require'.");
        }
    }

    private void validateJwtSecret(String jwtSecret, List<String> errors) {
        if (StringUtils.hasText(jwtSecret) && jwtSecret.length() < 32) {
            errors.add("JWT_SECRET must have at least 32 characters.");
        }
    }

    private void validateJwtExpiration(List<String> errors) {
        String rawValue = environment.getProperty("JWT_EXPIRATION_SECONDS");
        if (!StringUtils.hasText(rawValue)) {
            return;
        }

        try {
            long parsedValue = Long.parseLong(rawValue.trim());
            if (parsedValue <= 0) {
                errors.add("JWT_EXPIRATION_SECONDS must be a positive number.");
            }
        } catch (NumberFormatException ex) {
            errors.add("JWT_EXPIRATION_SECONDS must be a valid integer.");
        }
    }

    private String buildValidationMessage(Set<String> profiles, List<String> errors) {
        StringBuilder builder = new StringBuilder();
        builder.append(ENV_VALIDATION_FAILURE_PREFIX)
                .append(" for profiles ")
                .append(profiles)
                .append(".\n")
                .append("The application cannot start until the following issues are fixed:\n");

        errors.forEach(error -> builder.append(" - ").append(error).append('\n'));

        builder.append("Tip: update the corresponding .env file (for example '.env.dev') ")
                .append("or export these variables in your runtime environment.");
        return builder.toString();
    }
}
