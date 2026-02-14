package com.fixpoint.business.tickets.service;

import com.fixpoint.business.clients.repository.ClientRepository;
import com.fixpoint.business.clients.service.ClientServiceImpl;
import com.fixpoint.business.tickets.domain.TicketStatus;
import com.fixpoint.business.tickets.entity.Ticket;
import com.fixpoint.business.tickets.dto.CreateTicketDTO;
import com.fixpoint.business.tickets.dto.TicketDTO;
import com.fixpoint.business.tickets.repository.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    public static final String TICKET_NOT_FOUND = "Ticket not found";
    private final TicketRepository ticketRepository;
    private final ClientRepository clientRepository;

    @Override
    public List<TicketDTO> getAll() {
        return ticketRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
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

        return toDTO(ticketRepository.save(ticket));
    }

    @Override
    public TicketDTO update(Long id, CreateTicketDTO dto) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(TICKET_NOT_FOUND));

        var client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new EntityNotFoundException(ClientServiceImpl.CLIENT_NOT_FOUND));

        TicketStatus currentStatus = TicketStatus.parse(ticket.getStatus());
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

        return toDTO(ticketRepository.save(ticket));
    }

    @Override
    public void delete(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new EntityNotFoundException(TICKET_NOT_FOUND);
        }
        ticketRepository.deleteById(id);
    }

    @Override
    public List<TicketDTO> getByClientId(Long clientId) {
        return ticketRepository.findByClientId(clientId).stream().map(this::toDTO).toList();
    }

    @Override
    public List<TicketDTO> getByStatus(String status) {
        TicketStatus parsedStatus = TicketStatus.parse(status);
        return ticketRepository.findByStatus(parsedStatus.value()).stream().map(this::toDTO).toList();
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
