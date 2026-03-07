package com.fixpoint.business.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Pattern(regexp = "new|used|damaged")
    @Size(max = 20)
    private String condition;
    private String source;
    @NotNull
    @Min(1)
    private Integer quantity;
    private String location;
    private LocalDateTime addedAt;
}
