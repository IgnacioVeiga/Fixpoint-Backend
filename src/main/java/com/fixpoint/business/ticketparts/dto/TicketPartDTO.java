package com.fixpoint.business.ticketparts.dto;

public record TicketPartDTO(
        Long id,
        Long inventoryId,
        String inventoryName,
        Integer quantity,
        String note
) {}
