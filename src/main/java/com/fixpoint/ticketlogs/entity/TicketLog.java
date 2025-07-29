package com.fixpoint.ticketlogs.entity;

import com.fixpoint.tickets.entity.Ticket;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Registro de eventos o actualizaciones por cada ticket

@Entity
@Table(name = "ticket_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String author;
}