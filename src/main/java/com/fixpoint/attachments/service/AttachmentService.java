package com.fixpoint.attachments.service;

import com.fixpoint.attachments.dto.AttachmentDTO;

import java.util.List;

public interface AttachmentService {
    List<AttachmentDTO> findByTicketId(Long ticketId);
    AttachmentDTO findById(Long id);
    AttachmentDTO save(AttachmentDTO dto);
    void delete(Long id);
}