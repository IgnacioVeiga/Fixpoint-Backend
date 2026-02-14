package com.fixpoint.business.ticketparts.service;

import com.fixpoint.business.inventory.entity.Inventory;
import com.fixpoint.business.inventory.repository.InventoryRepository;
import com.fixpoint.business.ticketparts.dto.AddTicketPartDTO;
import com.fixpoint.business.ticketparts.dto.TicketPartDTO;
import com.fixpoint.business.ticketparts.entity.TicketPart;
import com.fixpoint.business.ticketparts.repository.TicketPartRepository;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketPartServiceTest {

    @Mock
    private TicketPartRepository ticketPartRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private TicketPartService service;

    @Test
    void addPartToTicketShouldDecreaseInventoryStockAndReturnDto() {
        Long ticketId = 10L;
        Long inventoryId = 5L;

        Ticket ticket = Ticket.builder().id(ticketId).build();
        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .name("Display A12")
                .condition("new")
                .quantity(8)
                .build();

        AddTicketPartDTO dto = new AddTicketPartDTO(inventoryId, 3, "Replaced cracked screen");

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketPartRepository.save(any(TicketPart.class))).thenAnswer(invocation -> {
            TicketPart part = invocation.getArgument(0);
            part.setId(99L);
            return part;
        });

        TicketPartDTO result = service.addPartToTicket(ticketId, dto);

        assertEquals(99L, result.id());
        assertEquals(inventoryId, result.inventoryId());
        assertEquals("Display A12", result.inventoryName());
        assertEquals(3, result.quantity());
        assertEquals("Replaced cracked screen", result.note());
        assertEquals(5, inventory.getQuantity());

        ArgumentCaptor<TicketPart> savedPartCaptor = ArgumentCaptor.forClass(TicketPart.class);
        verify(ticketPartRepository).save(savedPartCaptor.capture());
        assertEquals(ticketId, savedPartCaptor.getValue().getTicket().getId());
        assertEquals(inventoryId, savedPartCaptor.getValue().getInventory().getId());
    }

    @Test
    void addPartToTicketShouldThrowWhenStockIsInsufficient() {
        Long ticketId = 20L;
        Long inventoryId = 8L;

        Ticket ticket = Ticket.builder().id(ticketId).build();
        Inventory inventory = Inventory.builder()
                .id(inventoryId)
                .name("Battery B9")
                .condition("new")
                .quantity(1)
                .build();

        AddTicketPartDTO dto = new AddTicketPartDTO(inventoryId, 2, null);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.addPartToTicket(ticketId, dto)
        );

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        verify(ticketPartRepository, never()).save(any(TicketPart.class));
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void addPartToTicketShouldThrowWhenInventoryDoesNotExist() {
        Long ticketId = 30L;
        Long inventoryId = 12L;

        Ticket ticket = Ticket.builder().id(ticketId).build();
        AddTicketPartDTO dto = new AddTicketPartDTO(inventoryId, 1, null);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> service.addPartToTicket(ticketId, dto)
        );

        assertEquals(TicketPartService.INVENTORY_ITEM_NOT_FOUND, ex.getMessage());
    }

    @Test
    void getPartsForTicketShouldMapEntitiesToDto() {
        Long ticketId = 40L;

        Inventory inventory = Inventory.builder()
                .id(100L)
                .name("Fan X1")
                .condition("used")
                .quantity(4)
                .build();

        TicketPart ticketPart = TicketPart.builder()
                .id(200L)
                .inventory(inventory)
                .quantity(1)
                .note("Cleaned and reused")
                .build();

        when(ticketPartRepository.findByTicketId(ticketId)).thenReturn(List.of(ticketPart));

        List<TicketPartDTO> result = service.getPartsForTicket(ticketId);

        assertEquals(1, result.size());
        assertEquals(200L, result.getFirst().id());
        assertEquals(100L, result.getFirst().inventoryId());
        assertEquals("Fan X1", result.getFirst().inventoryName());
    }
}
