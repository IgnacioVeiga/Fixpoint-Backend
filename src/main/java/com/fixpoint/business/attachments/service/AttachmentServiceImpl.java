package com.fixpoint.business.attachments.service;

import com.fixpoint.business.attachments.entity.Attachment;
import com.fixpoint.business.attachments.dto.AttachmentDTO;
import com.fixpoint.business.attachments.repository.AttachmentRepository;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import com.fixpoint.business.tickets.service.TicketServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepo;
    private final TicketRepository ticketRepo;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public List<AttachmentDTO> findByTicketId(Long ticketId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException(TicketServiceImpl.TICKET_NOT_FOUND));

        return attachmentRepo.findByTicket(ticket).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public AttachmentDTO findById(Long id) {
        return attachmentRepo.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
    }

    @Override
    public AttachmentDTO save(AttachmentDTO dto) {
        Ticket ticket = ticketRepo.findById(dto.getTicketId())
                .orElseThrow(() -> new RuntimeException(TicketServiceImpl.TICKET_NOT_FOUND));

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .filename(dto.getFilename())
                .filepath(dto.getFilepath())
                .fileType(dto.getFileType())
                .build();

        return toDto(attachmentRepo.save(attachment));
    }

    @Override
    public void delete(Long id) {
        attachmentRepo.deleteById(id);
    }

    private AttachmentDTO toDto(Attachment a) {
        return AttachmentDTO.builder()
                .id(a.getId())
                .ticketId(a.getTicket().getId())
                .filename(a.getFilename())
                .filepath(a.getFilepath())
                .fileType(a.getFileType())
                .uploadedAt(a.getUploadedAt().format(formatter))
                .build();
    }
}
