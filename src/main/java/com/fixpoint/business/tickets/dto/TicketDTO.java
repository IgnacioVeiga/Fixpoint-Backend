package com.fixpoint.business.tickets.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TicketDTO(
        Long id,
        Long clientId,
        String deviceType,
        String brand,
        String model,
        String serialNumber,
        LocalDate entryDate,
        String problemDescription,
        String status,
        boolean needsContract,
        boolean contractSigned,
        String createdBy,
        LocalDateTime lastUpdated
) {}
