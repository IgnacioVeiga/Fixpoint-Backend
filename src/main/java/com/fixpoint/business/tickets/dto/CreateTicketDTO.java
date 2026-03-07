package com.fixpoint.business.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTicketDTO(
        @NotNull Long clientId,
        @NotBlank String deviceType,
        String brand,
        String model,
        String serialNumber,
        LocalDate entryDate,
        String problemDescription,
        @Size(max = 30) String status,
        boolean needsContract,
        boolean contractSigned,
        String createdBy
) {}
