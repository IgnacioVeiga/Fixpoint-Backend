package com.fixpoint.business.attachments.service;

import com.fixpoint.business.attachments.dto.AttachmentDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {
    List<AttachmentDTO> findByTicketId(Long ticketId);
    AttachmentDTO findById(Long id);
    AttachmentDTO save(AttachmentDTO dto);
    AttachmentDTO uploadFile(Long ticketId, MultipartFile file, String fileType);
    Resource downloadFile(Long id);
    void delete(Long id);
    AttachmentDTO replaceFile(Long id, MultipartFile file);
}
