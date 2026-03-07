package com.fixpoint.business.clients.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientDTO(
        @NotBlank String name,
        @Size(max = 20) String dni,
        @Size(max = 30) String phone,
        String email,
        String address,
        String notes
) {}
