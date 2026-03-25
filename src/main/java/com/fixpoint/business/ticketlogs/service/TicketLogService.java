package com.fixpoint.business.ticketlogs.service;

import com.fixpoint.business.ticketlogs.dto.CreateTicketLogDTO;
import com.fixpoint.business.ticketlogs.dto.TicketLogDTO;
import com.fixpoint.business.ticketlogs.entity.TicketLog;
import com.fixpoint.business.ticketlogs.repository.TicketLogRepository;
import com.fixpoint.business.tickets.domain.TicketStatus;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import com.fixpoint.business.tickets.service.TicketServiceImpl;
import com.fixpoint.config.cache.CacheInvalidationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketLogService {

    private final TicketLogRepository ticketLogRepository;
    private final TicketRepository ticketRepository;
    private final CacheInvalidationService cacheInvalidationService;

    @Transactional
    public TicketLogDTO createLog(Long ticketId, CreateTicketLogDTO dto) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException(TicketServiceImpl.TICKET_NOT_FOUND));

        TicketStatus ticketStatus = TicketStatus.parse(ticket.getStatus());
        if (ticketStatus.isClosed()) {
            throw new IllegalStateException("Cannot add logs to a closed ticket");
        }

        LocalDateTime now = LocalDateTime.now();
        TicketLog log = TicketLog.builder()
                .ticket(ticket)
                .description(dto.description())
                .author(dto.author())
                .timestamp(now)
                .build();

        TicketLog saved = ticketLogRepository.save(log);
        ticket.setLastUpdated(now);
        ticketRepository.save(ticket);
        if (cacheInvalidationService != null) {
            cacheInvalidationService.evictTickets();
        }
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
