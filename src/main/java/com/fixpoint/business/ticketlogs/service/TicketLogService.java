package com.fixpoint.business.ticketlogs.service;

import com.fixpoint.business.ticketlogs.dto.CreateTicketLogDTO;
import com.fixpoint.business.ticketlogs.dto.TicketLogDTO;
import com.fixpoint.business.ticketlogs.entity.TicketLog;
import com.fixpoint.business.ticketlogs.repository.TicketLogRepository;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import com.fixpoint.business.tickets.service.TicketServiceImpl;
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
                .orElseThrow(() -> new IllegalArgumentException(TicketServiceImpl.TICKET_NOT_FOUND));

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
