package com.fixpoint.business.ticketlogs.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTicketLogDTO(
        @NotBlank String description,
        String author
) {}
