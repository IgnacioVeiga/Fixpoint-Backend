package com.fixpoint.clients.service;

import com.fixpoint.clients.dto.ClientDTO;
import com.fixpoint.clients.dto.CreateClientDTO;
import com.fixpoint.clients.entity.Client;
import com.fixpoint.clients.repository.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepository repo;

    public ClientServiceImpl(ClientRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<ClientDTO> getAll() {
        return repo.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public ClientDTO getById(Long id) {
        return repo.findById(id).map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));
    }

    @Override
    public ClientDTO create(CreateClientDTO dto) {
        Client client = new Client();
        applyData(client, dto);
        return toDTO(repo.save(client));
    }

    @Override
    public ClientDTO update(Long id, CreateClientDTO dto) {
        Client client = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));
        applyData(client, dto);
        return toDTO(repo.save(client));
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new EntityNotFoundException("Client not found");
        }
        repo.deleteById(id);
    }

    @Override
    public List<ClientDTO> searchByName(String name) {
        return repo.findByNameContainingIgnoreCase(name).stream().map(this::toDTO).toList();
    }

    private void applyData(Client client, CreateClientDTO dto) {
        client.setName(dto.name());
        client.setDni(dto.dni());
        client.setPhone(dto.phone());
        client.setEmail(dto.email());
        client.setAddress(dto.address());
        client.setNotes(dto.notes());
    }

    private ClientDTO toDTO(Client c) {
        return new ClientDTO(
                c.getId(), c.getName(), c.getDni(), c.getPhone(),
                c.getEmail(), c.getAddress(), c.getNotes()
        );
    }
}