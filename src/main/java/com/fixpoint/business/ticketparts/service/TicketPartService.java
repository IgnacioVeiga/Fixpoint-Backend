package com.fixpoint.business.ticketparts.service;

import com.fixpoint.business.inventory.entity.Inventory;
import com.fixpoint.business.inventory.repository.InventoryRepository;
import com.fixpoint.business.ticketparts.dto.AddTicketPartDTO;
import com.fixpoint.business.ticketparts.dto.TicketPartDTO;
import com.fixpoint.business.ticketparts.entity.TicketPart;
import com.fixpoint.business.ticketparts.repository.TicketPartRepository;
import com.fixpoint.business.tickets.domain.TicketStatus;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.repository.TicketRepository;
import com.fixpoint.business.tickets.service.TicketServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketPartService {

    public static final String INVENTORY_ITEM_NOT_FOUND = "Inventory item not found";
    private final TicketPartRepository ticketPartRepository;
    private final TicketRepository ticketRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public TicketPartDTO addPartToTicket(Long ticketId, AddTicketPartDTO dto) {
        if (dto.quantity() == null || dto.quantity() < 1) {
            throw new IllegalArgumentException("Part quantity must be greater than zero");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException(TicketServiceImpl.TICKET_NOT_FOUND));

        TicketStatus ticketStatus = TicketStatus.parse(ticket.getStatus());
        if (ticketStatus.isClosed()) {
            throw new IllegalStateException("Cannot add parts to a closed ticket");
        }

        Inventory inventory = inventoryRepository.findById(dto.inventoryId())
                .orElseThrow(() -> new EntityNotFoundException(INVENTORY_ITEM_NOT_FOUND));

        if (inventory.getQuantity() < dto.quantity()) {
            throw new IllegalStateException("Insufficient stock for inventory item");
        }

        inventory.setQuantity(inventory.getQuantity() - dto.quantity());
        inventoryRepository.save(inventory);

        TicketPart part = TicketPart.builder()
                .ticket(ticket)
                .inventory(inventory)
                .quantity(dto.quantity())
                .note(dto.note())
                .build();

        TicketPart saved = ticketPartRepository.save(part);

        ticket.setLastUpdated(LocalDateTime.now());
        ticketRepository.save(ticket);

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
                part.getInventory().getName(),
                part.getQuantity(),
                part.getNote()
        );
    }
}
