package com.fixpoint.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Inventario del taller (componentes y piezas)

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "component_type")
    private String componentType;

    private String description;

    @Column(nullable = false)
    private String condition; // new, used, damaged

    private String source;

    @Column(nullable = false)
    private Integer quantity = 1;

    private String location;

    @Column(name = "added_at")
    private LocalDateTime addedAt = LocalDateTime.now();
}
