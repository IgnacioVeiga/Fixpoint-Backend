package com.fixpoint.business.tickets.entity;

import com.fixpoint.business.clients.entity.Client;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Tickets de reparación asociados a clientes

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "device_type", nullable = false)
    private String deviceType;

    private String brand;
    private String model;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "entry_date")
    private LocalDate entryDate = LocalDate.now();

    @Column(name = "problem_description", columnDefinition = "TEXT")
    private String problemDescription;

    private String status = "received";

    @Column(name = "needs_contract")
    private boolean needsContract = false;

    @Column(name = "contract_signed")
    private boolean contractSigned = false;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();
}
