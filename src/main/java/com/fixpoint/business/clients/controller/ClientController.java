package com.fixpoint.business.clients.controller;

import com.fixpoint.business.clients.dto.ClientDTO;
import com.fixpoint.business.clients.dto.CreateClientDTO;
import com.fixpoint.business.clients.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService service;

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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<ClientDTO> search(@RequestParam String name) {
        return service.searchByName(name);
    }
}
