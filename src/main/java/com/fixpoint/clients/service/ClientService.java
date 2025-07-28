package com.fixpoint.clients.service;

import com.fixpoint.clients.dto.ClientDTO;
import com.fixpoint.clients.dto.CreateClientDTO;

import java.util.List;

public interface ClientService {
    List<ClientDTO> getAll();
    ClientDTO getById(Long id);
    ClientDTO create(CreateClientDTO dto);
    ClientDTO update(Long id, CreateClientDTO dto);
    void delete(Long id);
    List<ClientDTO> searchByName(String name);
}