package com.fixpoint.business.ticketparts.repository;

import com.fixpoint.business.ticketparts.entity.TicketPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketPartRepository extends JpaRepository<TicketPart, Long> {
    List<TicketPart> findByTicketId(Long ticketId);
    boolean existsByInventoryId(Long inventoryId);
}
