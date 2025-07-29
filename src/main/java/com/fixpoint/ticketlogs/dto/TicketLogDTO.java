package com.fixpoint.ticketlogs.dto;

import java.time.LocalDateTime;

public record TicketLogDTO(
        Long id,
        Long ticketId,
        String description,
        String author,
        LocalDateTime timestamp
) {}