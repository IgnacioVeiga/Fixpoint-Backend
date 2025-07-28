package com.fixpoint.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateTicketDTO(
        @NotNull Long clientId,
        @NotBlank String deviceType,
        String brand,
        String model,
        String serialNumber,
        LocalDate entryDate,
        String problemDescription,
        String status,
        boolean needsContract,
        boolean contractSigned,
        String createdBy
) {}