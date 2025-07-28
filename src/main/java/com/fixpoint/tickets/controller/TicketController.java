package com.fixpoint.tickets.controller;

import com.fixpoint.tickets.dto.CreateTicketDTO;
import com.fixpoint.tickets.dto.TicketDTO;
import com.fixpoint.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    public List<TicketDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public TicketDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public TicketDTO create(@Valid @RequestBody CreateTicketDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public TicketDTO update(@PathVariable Long id, @Valid @RequestBody CreateTicketDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/client/{clientId}")
    public List<TicketDTO> getByClientId(@PathVariable Long clientId) {
        return service.getByClientId(clientId);
    }

    @GetMapping("/status")
    public List<TicketDTO> getByStatus(@RequestParam String status) {
        return service.getByStatus(status);
    }
}