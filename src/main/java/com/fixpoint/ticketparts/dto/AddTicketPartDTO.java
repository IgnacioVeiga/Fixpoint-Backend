package com.fixpoint.ticketparts.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddTicketPartDTO(
        @NotNull Long inventoryId,
        @Min(1) Integer quantity,
        String note
) {}