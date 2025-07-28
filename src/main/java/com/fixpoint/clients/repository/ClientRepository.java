package com.fixpoint.clients.repository;

import com.fixpoint.clients.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByNameContainingIgnoreCase(String name);
    boolean existsByDni(String dni);
}