package com.fixpoint.clients.dto;

public record ClientDTO(
        Long id,
        String name,
        String dni,
        String phone,
        String email,
        String address,
        String notes
) {}