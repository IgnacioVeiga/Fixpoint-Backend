package com.fixpoint.business.attachments.entity;

import com.fixpoint.business.tickets.entity.Ticket;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Archivos adjuntos relacionados a tickets (fotos, PDFs, etc.)

@Entity
@Table(name = "attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String filepath;

    @Column(name = "file_type", nullable = false)
    private String fileType; // photo, contract, invoice, other

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
