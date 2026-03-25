package com.fixpoint.business.tickets.service;

import com.fixpoint.business.attachments.repository.AttachmentRepository;
import com.fixpoint.business.clients.repository.ClientRepository;
import com.fixpoint.business.clients.service.ClientServiceImpl;
import com.fixpoint.business.ticketlogs.repository.TicketLogRepository;
import com.fixpoint.business.ticketparts.repository.TicketPartRepository;
import com.fixpoint.business.tickets.domain.TicketStatus;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.dto.CreateTicketDTO;
import com.fixpoint.business.tickets.dto.TicketDTO;
import com.fixpoint.business.tickets.dto.TicketStatusDefinitionDTO;
import com.fixpoint.business.tickets.repository.TicketRepository;
import com.fixpoint.config.cache.CacheInvalidationService;
import com.fixpoint.config.cache.CacheNames;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    public static final String TICKET_NOT_FOUND = "Ticket not found";
    public static final String CLOSED_TICKETS_CANNOT_BE_DELETED = "Closed tickets cannot be deleted";
    public static final String CANNOT_DELETE_TICKET_WITH_RELATED_DATA = "Cannot delete ticket with related parts, logs, or attachments";
    private final TicketRepository ticketRepository;
    private final ClientRepository clientRepository;
    private final TicketPartRepository ticketPartRepository;
    private final TicketLogRepository ticketLogRepository;
    private final AttachmentRepository attachmentRepository;
    private final CacheInvalidationService cacheInvalidationService;

    @Override
    @Cacheable(CacheNames.TICKETS_ALL)
    public List<TicketDTO> getAll() {
        return ticketRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.TICKET_BY_ID, key = "#id")
    public TicketDTO getById(Long id) {
        return ticketRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(TICKET_NOT_FOUND));
    }

    @Override
    public TicketDTO create(CreateTicketDTO dto) {
        var client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException(ClientServiceImpl.CLIENT_NOT_FOUND));

        TicketStatus status = TicketStatus.parseOrDefault(dto.status(), TicketStatus.RECEIVED);
        validateContractConsistency(dto.needsContract(), dto.contractSigned(), status);

        Ticket ticket = Ticket.builder()
                .client(client)
                .deviceType(dto.deviceType())
                .brand(dto.brand())
                .model(dto.model())
                .serialNumber(dto.serialNumber())
                .entryDate(dto.entryDate() != null ? dto.entryDate() : LocalDate.now())
                .problemDescription(dto.problemDescription())
                .status(status.value())
                .needsContract(dto.needsContract())
                .contractSigned(dto.contractSigned())
                .createdBy(dto.createdBy())
                .lastUpdated(LocalDateTime.now())
                .build();

        TicketDTO savedTicket = toDTO(ticketRepository.save(ticket));
        evictTicketCaches();
        return savedTicket;
    }

    @Override
    public TicketDTO update(Long id, CreateTicketDTO dto) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(TICKET_NOT_FOUND));

        var client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException(ClientServiceImpl.CLIENT_NOT_FOUND));

        TicketStatus currentStatus = TicketStatus.parse(ticket.getStatus());
        if (currentStatus.isClosed()) {
            throw new IllegalStateException("Closed tickets cannot be edited");
        }

        TicketStatus nextStatus = TicketStatus.parseOrDefault(dto.status(), currentStatus);
        validateStatusTransition(currentStatus, nextStatus);
        validateContractConsistency(dto.needsContract(), dto.contractSigned(), nextStatus);

        ticket.setClient(client);
        ticket.setDeviceType(dto.deviceType());
        ticket.setBrand(dto.brand());
        ticket.setModel(dto.model());
        ticket.setSerialNumber(dto.serialNumber());
        ticket.setEntryDate(dto.entryDate() != null ? dto.entryDate() : ticket.getEntryDate());
        ticket.setProblemDescription(dto.problemDescription());
        ticket.setStatus(nextStatus.value());
        ticket.setNeedsContract(dto.needsContract());
        ticket.setContractSigned(dto.contractSigned());
        ticket.setCreatedBy(dto.createdBy());
        ticket.setLastUpdated(LocalDateTime.now());

        TicketDTO updatedTicket = toDTO(ticketRepository.save(ticket));
        evictTicketCaches();
        return updatedTicket;
    }

    @Override
    public void delete(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(TICKET_NOT_FOUND));

        TicketStatus currentStatus = TicketStatus.parse(ticket.getStatus());
        if (currentStatus.isClosed()) {
            throw new IllegalStateException(CLOSED_TICKETS_CANNOT_BE_DELETED);
        }

        if (ticketPartRepository.existsByTicketId(id)
                || ticketLogRepository.existsByTicketId(id)
                || attachmentRepository.existsByTicketId(id)) {
            throw new IllegalStateException(CANNOT_DELETE_TICKET_WITH_RELATED_DATA);
        }

        ticketRepository.deleteById(id);
        evictTicketCaches();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.TICKETS_BY_CLIENT, key = "#clientId")
    public List<TicketDTO> getByClientId(Long clientId) {
        return ticketRepository.findByClientId(clientId).stream().map(this::toDTO).toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.TICKETS_BY_STATUS, key = "#status")
    public List<TicketDTO> getByStatus(String status) {
        TicketStatus parsedStatus = TicketStatus.parse(status);
        return ticketRepository.findByStatus(parsedStatus.value()).stream().map(this::toDTO).toList();
    }

    @Override
    @Cacheable(CacheNames.TICKET_STATUS_DEFINITIONS)
    public List<TicketStatusDefinitionDTO> getStatusDefinitions() {
        return Arrays.stream(TicketStatus.values())
                .map(status -> new TicketStatusDefinitionDTO(
                        status.value(),
                        status.isClosed(),
                        status.nextStatuses()
                ))
                .toList();
    }

    private void validateStatusTransition(TicketStatus currentStatus, TicketStatus nextStatus) {
        if (!currentStatus.canTransitionTo(nextStatus)) {
            throw new IllegalStateException("Invalid ticket status transition from '" +
                    currentStatus.value() + "' to '" + nextStatus.value() + "'");
        }
    }

    private void validateContractConsistency(boolean needsContract, boolean contractSigned, TicketStatus status) {
        if (!needsContract && contractSigned) {
            throw new IllegalArgumentException("contractSigned requires needsContract=true");
        }

        if (status == TicketStatus.RETURNED && needsContract && !contractSigned) {
            throw new IllegalStateException("Cannot mark ticket as returned without signed contract");
        }
    }

    private void evictTicketCaches() {
        if (cacheInvalidationService == null) {
            return;
        }

        cacheInvalidationService.evictTickets();
        cacheInvalidationService.evictDashboard();
    }

    private TicketDTO toDTO(Ticket t) {
        return new TicketDTO(
                t.getId(),
                t.getClient().getId(),
                t.getDeviceType(),
                t.getBrand(),
                t.getModel(),
                t.getSerialNumber(),
                t.getEntryDate(),
                t.getProblemDescription(),
                t.getStatus(),
                t.isNeedsContract(),
                t.isContractSigned(),
                t.getCreatedBy(),
                t.getLastUpdated()
        );
    }
}
