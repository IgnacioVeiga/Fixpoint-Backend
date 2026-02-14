package com.fixpoint.business.tickets.service;

import com.fixpoint.business.clients.entity.Client;
import com.fixpoint.business.clients.repository.ClientRepository;
import com.fixpoint.business.tickets.dto.CreateTicketDTO;
import com.fixpoint.business.tickets.dto.TicketDTO;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private TicketServiceImpl service;

    @Test
    void createShouldRejectSignedContractWhenContractIsNotRequired() {
        Client client = Client.builder().id(1L).name("Alice").build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        CreateTicketDTO dto = new CreateTicketDTO(
                1L,
                "Laptop",
                null,
                null,
                null,
                LocalDate.of(2026, 2, 14),
                "No power",
                "received",
                false,
                true,
                "tech"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(dto));

        assertEquals("contractSigned requires needsContract=true", ex.getMessage());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void updateShouldRejectInvalidStatusTransition() {
        Client client = Client.builder().id(1L).name("Alice").build();
        Ticket existing = Ticket.builder()
                .id(99L)
                .client(client)
                .deviceType("Laptop")
                .entryDate(LocalDate.of(2026, 2, 14))
                .status("received")
                .needsContract(false)
                .contractSigned(false)
                .lastUpdated(LocalDateTime.of(2026, 2, 14, 10, 0))
                .build();

        when(ticketRepository.findById(99L)).thenReturn(Optional.of(existing));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        CreateTicketDTO dto = new CreateTicketDTO(
                1L,
                "Laptop",
                null,
                null,
                null,
                LocalDate.of(2026, 2, 14),
                "No power",
                "repaired",
                false,
                false,
                "tech"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.update(99L, dto));

        assertEquals("Invalid ticket status transition from 'received' to 'repaired'", ex.getMessage());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void updateShouldRejectReturnedStatusWhenContractIsPending() {
        Client client = Client.builder().id(1L).name("Alice").build();
        Ticket existing = Ticket.builder()
                .id(99L)
                .client(client)
                .deviceType("Laptop")
                .entryDate(LocalDate.of(2026, 2, 14))
                .status("repaired")
                .needsContract(true)
                .contractSigned(false)
                .lastUpdated(LocalDateTime.of(2026, 2, 14, 10, 0))
                .build();

        when(ticketRepository.findById(99L)).thenReturn(Optional.of(existing));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        CreateTicketDTO dto = new CreateTicketDTO(
                1L,
                "Laptop",
                null,
                null,
                null,
                LocalDate.of(2026, 2, 14),
                "Repaired and ready",
                "returned",
                true,
                false,
                "tech"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.update(99L, dto));

        assertEquals("Cannot mark ticket as returned without signed contract", ex.getMessage());
    }

    @Test
    void updateShouldAllowValidTransition() {
        Client client = Client.builder().id(1L).name("Alice").build();
        Ticket existing = Ticket.builder()
                .id(100L)
                .client(client)
                .deviceType("Laptop")
                .entryDate(LocalDate.of(2026, 2, 14))
                .status("diagnosing")
                .needsContract(false)
                .contractSigned(false)
                .lastUpdated(LocalDateTime.of(2026, 2, 14, 10, 0))
                .build();

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTicketDTO dto = new CreateTicketDTO(
                1L,
                "Laptop",
                "Lenovo",
                "T14",
                "SN-10",
                LocalDate.of(2026, 2, 14),
                "Replacing battery",
                "repairing",
                false,
                false,
                "tech"
        );

        TicketDTO result = service.update(100L, dto);

        assertEquals("repairing", result.status());
        verify(ticketRepository).save(existing);
    }

    @Test
    void getByStatusShouldRejectUnknownStatus() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.getByStatus("unknown"));
        assertEquals("Invalid ticket status 'unknown'. Allowed values: received, diagnosing, waiting_parts, repairing, repaired, returned, cancelled", ex.getMessage());
    }

    @Test
    void getByStatusShouldNormalizeValueBeforeQueryingRepository() {
        Client client = Client.builder().id(1L).name("Alice").build();
        Ticket ticket = Ticket.builder()
                .id(50L)
                .client(client)
                .deviceType("Laptop")
                .entryDate(LocalDate.of(2026, 2, 14))
                .status("diagnosing")
                .needsContract(false)
                .contractSigned(false)
                .lastUpdated(LocalDateTime.of(2026, 2, 14, 11, 0))
                .build();

        when(ticketRepository.findByStatus("diagnosing")).thenReturn(List.of(ticket));

        List<TicketDTO> result = service.getByStatus("DIAGNOSING");

        assertEquals(1, result.size());
        assertEquals("diagnosing", result.getFirst().status());
        verify(ticketRepository).findByStatus("diagnosing");
    }
}
