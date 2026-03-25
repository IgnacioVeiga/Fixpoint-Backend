package com.fixpoint.business.attachments.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentDTO {
    private Long id;
    private Long ticketId;
    private String filename;
    private String filepath;
    private String fileType;
    private String fileFormat;
    private Long fileSizeBytes;
    private String tag;
    private LocalDateTime uploadedAt;
}
