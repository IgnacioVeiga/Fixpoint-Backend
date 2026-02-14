package com.fixpoint.business.tickets.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum TicketStatus {
    RECEIVED("received"),
    DIAGNOSING("diagnosing"),
    WAITING_PARTS("waiting_parts"),
    REPAIRING("repairing"),
    REPAIRED("repaired"),
    RETURNED("returned"),
    CANCELLED("cancelled");

    private final String value;

    TicketStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean isClosed() {
        return this == RETURNED || this == CANCELLED;
    }

    public boolean canTransitionTo(TicketStatus target) {
        if (this == target) {
            return true;
        }

        return switch (this) {
            case RECEIVED -> target == DIAGNOSING || target == CANCELLED;
            case DIAGNOSING -> target == WAITING_PARTS || target == REPAIRING || target == CANCELLED;
            case WAITING_PARTS -> target == REPAIRING || target == CANCELLED;
            case REPAIRING -> target == WAITING_PARTS || target == REPAIRED || target == CANCELLED;
            case REPAIRED -> target == RETURNED || target == CANCELLED;
            case RETURNED, CANCELLED -> false;
        };
    }

    public static TicketStatus parse(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new IllegalArgumentException("Ticket status must not be blank");
        }

        String normalized = rawStatus.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(status -> status.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid ticket status '" + rawStatus + "'. Allowed values: " + allowedValues()
                ));
    }

    public static TicketStatus parseOrDefault(String rawStatus, TicketStatus fallback) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return fallback;
        }
        return parse(rawStatus);
    }

    public static String allowedValues() {
        return Arrays.stream(values())
                .map(TicketStatus::value)
                .collect(Collectors.joining(", "));
    }
}
