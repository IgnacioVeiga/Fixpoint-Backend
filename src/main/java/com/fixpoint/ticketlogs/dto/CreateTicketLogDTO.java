package com.fixpoint.ticketlogs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketLogDTO(
        @NotNull Long ticketId,
        @NotBlank String description,
        String author
) {}