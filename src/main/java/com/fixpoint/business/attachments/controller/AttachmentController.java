package com.fixpoint.business.attachments.controller;

import com.fixpoint.business.attachments.dto.AttachmentDTO;
import com.fixpoint.business.attachments.service.AttachmentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@Validated
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/recent")
    public List<AttachmentDTO> findRecent(
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int limit
    ) {
        return attachmentService.findRecent(limit);
    }

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
            @RequestParam(value = "tag", required = false) @Size(max = 80) String tag
    ) {
        return attachmentService.uploadFile(ticketId, file, tag);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource resource = attachmentService.downloadFile(id);
        AttachmentDTO attachment = attachmentService.findById(id);
        MediaType mediaType = MediaTypeFactory.getMediaType(attachment.getFilename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
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
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tag", required = false) @Size(max = 80) String tag
    ) {
        return attachmentService.replaceFile(id, file, tag);
    }
}
