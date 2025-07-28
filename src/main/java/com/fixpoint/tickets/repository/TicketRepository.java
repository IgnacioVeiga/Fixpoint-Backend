package com.fixpoint.tickets.repository;

import com.fixpoint.tickets.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByClientId(Long clientId);
    List<Ticket> findByStatus(String status);
}