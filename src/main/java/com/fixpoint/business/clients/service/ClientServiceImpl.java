package com.fixpoint.business.clients.service;

import com.fixpoint.business.clients.dto.ClientDTO;
import com.fixpoint.business.clients.dto.CreateClientDTO;
import com.fixpoint.business.clients.entity.Client;
import com.fixpoint.business.clients.repository.ClientRepository;
import com.fixpoint.config.cache.CacheInvalidationService;
import com.fixpoint.config.cache.CacheNames;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    public static final String CLIENT_NOT_FOUND = "Client not found";
    private final ClientRepository repo;
    private final CacheInvalidationService cacheInvalidationService;

    @Override
    @Cacheable(CacheNames.CLIENTS_ALL)
    public List<ClientDTO> getAll() {
        return repo.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CLIENT_BY_ID, key = "#id")
    public ClientDTO getById(Long id) {
        return repo.findById(id).map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(CLIENT_NOT_FOUND));
    }

    @Override
    public ClientDTO create(CreateClientDTO dto) {
        Client client = new Client();
        applyData(client, dto);
        ClientDTO savedClient = toDTO(repo.save(client));
        evictClientCaches();
        return savedClient;
    }

    @Override
    public ClientDTO update(Long id, CreateClientDTO dto) {
        Client client = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CLIENT_NOT_FOUND));
        applyData(client, dto);
        ClientDTO updatedClient = toDTO(repo.save(client));
        evictClientCaches();
        return updatedClient;
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new EntityNotFoundException(CLIENT_NOT_FOUND);
        }
        repo.deleteById(id);
        evictClientCaches();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CLIENTS_BY_NAME, key = "#name")
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

    private void evictClientCaches() {
        if (cacheInvalidationService == null) {
            return;
        }

        cacheInvalidationService.evictClients();
        cacheInvalidationService.evictDashboard();
    }

    private ClientDTO toDTO(Client c) {
        return new ClientDTO(
                c.getId(), c.getName(), c.getDni(), c.getPhone(),
                c.getEmail(), c.getAddress(), c.getNotes()
        );
    }
}
