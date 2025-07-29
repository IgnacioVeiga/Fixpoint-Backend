package com.fixpoint.ticketlogs.service;

import com.fixpoint.ticketlogs.dto.CreateTicketLogDTO;
import com.fixpoint.ticketlogs.dto.TicketLogDTO;
import com.fixpoint.ticketlogs.entity.TicketLog;
import com.fixpoint.ticketlogs.repository.TicketLogRepository;
import com.fixpoint.tickets.entity.Ticket;
import com.fixpoint.tickets.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketLogService {

    private final TicketLogRepository ticketLogRepository;
    private final TicketRepository ticketRepository;

    public TicketLogDTO createLog(CreateTicketLogDTO dto) {
        Ticket ticket = ticketRepository.findById(dto.ticketId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        TicketLog log = TicketLog.builder()
                .ticket(ticket)
                .description(dto.description())
                .author(dto.author())
                .timestamp(LocalDateTime.now())
                .build();

        TicketLog saved = ticketLogRepository.save(log);
        return toDTO(saved);
    }

    public List<TicketLogDTO> getLogsByTicketId(Long ticketId) {
        return ticketLogRepository.findByTicketIdOrderByTimestampDesc(ticketId)
                .stream().map(this::toDTO).toList();
    }

    private TicketLogDTO toDTO(TicketLog log) {
        return new TicketLogDTO(
                log.getId(),
                log.getTicket().getId(),
                log.getDescription(),
                log.getAuthor(),
                log.getTimestamp()
        );
    }
}