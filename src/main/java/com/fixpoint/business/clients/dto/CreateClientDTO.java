package com.fixpoint.business.clients.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClientDTO(
        @NotBlank String name,
        String dni,
        String phone,
        String email,
        String address,
        String notes
) {}
