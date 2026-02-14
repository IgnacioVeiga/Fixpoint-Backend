package com.fixpoint.business.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDTO {
    private Long id;
    @NotBlank
    private String name;
    private String componentType;
    private String description;
    @NotBlank
    private String condition;
    private String source;
    @NotNull
    @Min(1)
    private Integer quantity;
    private String location;
    private LocalDateTime addedAt;
}
