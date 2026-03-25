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
    private String fileType; // image, document, spreadsheet, archive, other

    @Column(name = "file_format", nullable = false)
    private String fileFormat;

    @Column(name = "file_size_bytes", nullable = false)
    @Builder.Default
    private Long fileSizeBytes = 0L;

    @Column(name = "tag")
    private String tag;

    @Column(name = "uploaded_at")
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
