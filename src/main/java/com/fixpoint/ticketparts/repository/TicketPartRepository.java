package com.fixpoint.ticketparts.repository;

import com.fixpoint.ticketparts.entity.TicketPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketPartRepository extends JpaRepository<TicketPart, Long> {
    List<TicketPart> findByTicketId(Long ticketId);
}