package com.fixpoint.business.tickets.dto;

import java.util.List;

public record TicketStatusDefinitionDTO(
        String value,
        boolean closed,
        List<String> nextStatuses
) {
}
