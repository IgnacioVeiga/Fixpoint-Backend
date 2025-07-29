package com.fixpoint.ticketparts.entity;

import com.fixpoint.inventory.entity.Inventory;
import com.fixpoint.tickets.entity.Ticket;
import jakarta.persistence.*;
import lombok.*;

// Relación entre tickets y componentes del inventario utilizados

@Entity
@Table(name = "ticket_parts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @Column(nullable = false)
    private Integer quantity = 1;

    private String note;
}