package com.fixpoint.clients.controller;

import com.fixpoint.clients.dto.ClientDTO;
import com.fixpoint.clients.dto.CreateClientDTO;
import com.fixpoint.clients.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClientDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ClientDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ClientDTO create(@Valid @RequestBody CreateClientDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ClientDTO update(@PathVariable Long id, @Valid @RequestBody CreateClientDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/search")
    public List<ClientDTO> search(@RequestParam String name) {
        return service.searchByName(name);
    }
}