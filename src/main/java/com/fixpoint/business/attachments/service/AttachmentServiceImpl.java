package com.fixpoint.business.attachments.service;

import com.fixpoint.business.attachments.entity.Attachment;
import com.fixpoint.business.attachments.dto.AttachmentDTO;
import com.fixpoint.business.attachments.repository.AttachmentRepository;
import com.fixpoint.business.tickets.domain.TicketStatus;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import com.fixpoint.business.tickets.service.TicketServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final String ATTACHMENT_NOT_FOUND = "Attachment not found";
    public static final String ORIGINAL_FILENAME_MUST_NOT_BE_NULL = "Original filename must not be null";

    private final AttachmentRepository attachmentRepo;
    private final TicketRepository ticketRepo;
    private final FileStorageService fileStorageService;

    @Override
    public List<AttachmentDTO> findByTicketId(Long ticketId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException(TicketServiceImpl.TICKET_NOT_FOUND));

        return attachmentRepo.findByTicket(ticket).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public AttachmentDTO findById(Long id) {
        return attachmentRepo.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException(ATTACHMENT_NOT_FOUND));
    }

    @Override
    public AttachmentDTO save(AttachmentDTO dto) {
        Ticket ticket = ticketRepo.findById(dto.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException(TicketServiceImpl.TICKET_NOT_FOUND));

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .filename(dto.getFilename())
                .filepath(dto.getFilepath())
                .fileType(dto.getFileType())
                .build();

        return toDto(attachmentRepo.save(attachment));
    }

    @Override
    public AttachmentDTO uploadFile(Long ticketId, MultipartFile file, String fileType) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException(TicketServiceImpl.TICKET_NOT_FOUND));
        ensureTicketIsOpen(ticket, "upload attachments");

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), ORIGINAL_FILENAME_MUST_NOT_BE_NULL);
        String storedFileName = fileStorageService.storeFile(file);

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .filename(originalFilename)
                .filepath(storedFileName)
                .fileType(fileType)
                .build();

        return toDto(attachmentRepo.save(attachment));
    }

    @Override
    public Resource downloadFile(Long id) {
        Attachment attachment = attachmentRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ATTACHMENT_NOT_FOUND));
        return fileStorageService.loadFileAsResource(attachment.getFilepath());
    }

    @Override
    public void delete(Long id) {
        Attachment attachment = attachmentRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ATTACHMENT_NOT_FOUND));
        ensureTicketIsOpen(attachment.getTicket(), "delete attachments");

        fileStorageService.deleteFile(attachment.getFilepath());
        attachmentRepo.delete(attachment);
    }

    @Override
    public AttachmentDTO replaceFile(Long id, MultipartFile file) {
        Attachment attachment = attachmentRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ATTACHMENT_NOT_FOUND));
        ensureTicketIsOpen(attachment.getTicket(), "replace attachments");

        // Delete old file
        fileStorageService.deleteFile(attachment.getFilepath());

        // Store new file
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), ORIGINAL_FILENAME_MUST_NOT_BE_NULL);
        String storedFileName = fileStorageService.storeFile(file);

        // Update attachment
        attachment.setFilename(originalFilename);
        attachment.setFilepath(storedFileName);

        return toDto(attachmentRepo.save(attachment));
    }

    private void ensureTicketIsOpen(Ticket ticket, String action) {
        TicketStatus status = TicketStatus.parse(ticket.getStatus());
        if (status.isClosed()) {
            throw new IllegalStateException("Cannot " + action + " for a closed ticket");
        }
    }

    private AttachmentDTO toDto(Attachment attachment) {
        return AttachmentDTO.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicket().getId())
                .filename(attachment.getFilename())
                .filepath(attachment.getFilepath())
                .fileType(attachment.getFileType())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
