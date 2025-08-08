package com.fixpoint.business.ticketparts.service;

import com.fixpoint.business.inventory.entity.Inventory;
import com.fixpoint.business.inventory.repository.InventoryRepository;
import com.fixpoint.business.ticketparts.dto.AddTicketPartDTO;
import com.fixpoint.business.ticketparts.dto.TicketPartDTO;
import com.fixpoint.business.ticketparts.entity.TicketPart;
import com.fixpoint.business.ticketparts.repository.TicketPartRepository;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import com.fixpoint.business.tickets.service.TicketServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketPartService {

    public static final String INVENTORY_ITEM_NOT_FOUND = "Inventory item not found";
    private final TicketPartRepository ticketPartRepository;
    private final TicketRepository ticketRepository;
    private final InventoryRepository inventoryRepository;

    public TicketPartDTO addPartToTicket(Long ticketId, AddTicketPartDTO dto) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException(TicketServiceImpl.TICKET_NOT_FOUND));

        Inventory inventory = inventoryRepository.findById(dto.inventoryId())
                .orElseThrow(() -> new IllegalArgumentException(INVENTORY_ITEM_NOT_FOUND));

        TicketPart part = TicketPart.builder()
                .ticket(ticket)
                .inventory(inventory)
                .quantity(dto.quantity())
                .note(dto.note())
                .build();

        TicketPart saved = ticketPartRepository.save(part);
        return toDTO(saved);
    }

    public List<TicketPartDTO> getPartsForTicket(Long ticketId) {
        return ticketPartRepository.findByTicketId(ticketId)
                .stream().map(this::toDTO).toList();
    }

    private TicketPartDTO toDTO(TicketPart part) {
        return new TicketPartDTO(
                part.getId(),
                part.getInventory().getId(),
                part.getInventory().getName(),  // asumo que Inventory tiene `name`
                part.getQuantity(),
                part.getNote()
        );
    }
}
