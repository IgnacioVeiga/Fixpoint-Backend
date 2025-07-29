package com.fixpoint.ticketparts.service;

import com.fixpoint.inventory.entity.Inventory;
import com.fixpoint.inventory.repository.InventoryRepository;
import com.fixpoint.ticketparts.dto.AddTicketPartDTO;
import com.fixpoint.ticketparts.dto.TicketPartDTO;
import com.fixpoint.ticketparts.entity.TicketPart;
import com.fixpoint.ticketparts.repository.TicketPartRepository;
import com.fixpoint.tickets.entity.Ticket;
import com.fixpoint.tickets.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketPartService {

    private final TicketPartRepository ticketPartRepository;
    private final TicketRepository ticketRepository;
    private final InventoryRepository inventoryRepository;

    public TicketPartDTO addPartToTicket(Long ticketId, AddTicketPartDTO dto) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        Inventory inventory = inventoryRepository.findById(dto.inventoryId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));

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