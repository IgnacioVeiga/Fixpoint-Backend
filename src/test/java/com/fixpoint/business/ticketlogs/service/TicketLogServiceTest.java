package com.fixpoint.business.ticketlogs.service;

import com.fixpoint.business.ticketlogs.dto.CreateTicketLogDTO;
import com.fixpoint.business.ticketlogs.dto.TicketLogDTO;
import com.fixpoint.business.ticketlogs.entity.TicketLog;
import com.fixpoint.business.ticketlogs.repository.TicketLogRepository;
import com.fixpoint.business.tickets.domain.TicketStatus;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketLogServiceTest {

    @Mock
    private TicketLogRepository ticketLogRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketLogService service;

    @Test
    void createLogShouldPersistLogAndUpdateTicketLastUpdated() {
        Long ticketId = 1L;
        LocalDateTime oldLastUpdated = LocalDateTime.of(2026, 1, 1, 10, 0);
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .status(TicketStatus.DIAGNOSING.value())
                .lastUpdated(oldLastUpdated)
                .build();

        CreateTicketLogDTO dto = new CreateTicketLogDTO("Checked motherboard", "tech-1");

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketLogRepository.save(any(TicketLog.class))).thenAnswer(invocation -> {
            TicketLog log = invocation.getArgument(0);
            log.setId(99L);
            return log;
        });
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketLogDTO result = service.createLog(ticketId, dto);

        assertEquals(99L, result.id());
        assertEquals(ticketId, result.ticketId());
        assertEquals("Checked motherboard", result.description());
        assertEquals("tech-1", result.author());
        assertNotEquals(oldLastUpdated, ticket.getLastUpdated());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void createLogShouldFailForClosedTicket() {
        Long ticketId = 2L;
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .status(TicketStatus.RETURNED.value())
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.createLog(ticketId, new CreateTicketLogDTO("Should fail", "tech"))
        );

        assertEquals("Cannot add logs to a closed ticket", ex.getMessage());
        verify(ticketLogRepository, never()).save(any(TicketLog.class));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }
}
