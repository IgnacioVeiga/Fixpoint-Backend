package com.fixpoint.attachments.dto;

import lombok.*;

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
    private String uploadedAt;
}