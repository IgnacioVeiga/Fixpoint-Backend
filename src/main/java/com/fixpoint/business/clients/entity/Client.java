package com.fixpoint.business.clients.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Clientes del sistema

@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String dni;
    private String phone;
    private String email;
    private String address;
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
