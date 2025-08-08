package com.fixpoint.business.attachments.controller;

import com.fixpoint.business.attachments.dto.AttachmentDTO;
import com.fixpoint.business.attachments.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService service;

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<AttachmentDTO>> getByTicketId(@PathVariable Long ticketId) {
        return ResponseEntity.ok(service.findByTicketId(ticketId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttachmentDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<AttachmentDTO> upload(@RequestBody AttachmentDTO dto) {
        return ResponseEntity.ok(service.save(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
