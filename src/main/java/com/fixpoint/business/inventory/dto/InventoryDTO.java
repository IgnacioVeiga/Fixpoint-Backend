package com.fixpoint.business.inventory.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDTO {
    private Long id;
    private String name;
    private String componentType;
    private String description;
    private String condition;
    private String source;
    private Integer quantity;
    private String location;
    private String addedAt;
}
