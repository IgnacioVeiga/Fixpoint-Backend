package com.fixpoint.business.tickets.controller;

import com.fixpoint.business.tickets.dto.CreateTicketDTO;
import com.fixpoint.business.tickets.dto.TicketDTO;
import com.fixpoint.business.tickets.dto.TicketStatusDefinitionDTO;
import com.fixpoint.business.tickets.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService service;

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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/client/{clientId}")
    public List<TicketDTO> getByClientId(@PathVariable Long clientId) {
        return service.getByClientId(clientId);
    }

    @GetMapping("/status")
    public List<TicketDTO> getByStatus(@RequestParam String status) {
        return service.getByStatus(status);
    }

    @GetMapping("/statuses")
    public List<TicketStatusDefinitionDTO> getStatuses() {
        return service.getStatusDefinitions();
    }
}
