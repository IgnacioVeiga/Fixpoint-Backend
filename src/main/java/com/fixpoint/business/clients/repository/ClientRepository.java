package com.fixpoint.business.clients.repository;

import com.fixpoint.business.clients.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByNameContainingIgnoreCase(String name);
    boolean existsByDni(String dni);
}
