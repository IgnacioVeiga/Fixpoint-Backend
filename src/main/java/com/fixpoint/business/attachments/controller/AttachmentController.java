package com.fixpoint.business.attachments.controller;

import com.fixpoint.business.attachments.dto.AttachmentDTO;
import com.fixpoint.business.attachments.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/ticket/{ticketId}")
    public List<AttachmentDTO> findByTicketId(@PathVariable Long ticketId) {
        return attachmentService.findByTicketId(ticketId);
    }

    @GetMapping("/{id}")
    public AttachmentDTO findById(@PathVariable Long id) {
        return attachmentService.findById(id);
    }

    @PostMapping("/upload/ticket/{ticketId}")
    public AttachmentDTO uploadFile(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") String fileType) {
        return attachmentService.uploadFile(ticketId, file, fileType);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource resource = attachmentService.downloadFile(id);
        AttachmentDTO attachment = attachmentService.findById(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        attachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/replace")
    public AttachmentDTO replaceFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return attachmentService.replaceFile(id, file);
    }
}
