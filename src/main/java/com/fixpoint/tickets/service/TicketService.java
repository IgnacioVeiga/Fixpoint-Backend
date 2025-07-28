package com.fixpoint.tickets.service;

import com.fixpoint.tickets.dto.CreateTicketDTO;
import com.fixpoint.tickets.dto.TicketDTO;

import java.util.List;

public interface TicketService {
    List<TicketDTO> getAll();
    TicketDTO getById(Long id);
    TicketDTO create(CreateTicketDTO dto);
    TicketDTO update(Long id, CreateTicketDTO dto);
    void delete(Long id);
    List<TicketDTO> getByClientId(Long clientId);
    List<TicketDTO> getByStatus(String status);
}