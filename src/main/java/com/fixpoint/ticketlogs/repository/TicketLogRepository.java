package com.fixpoint.ticketlogs.repository;

import com.fixpoint.ticketlogs.entity.TicketLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketLogRepository extends JpaRepository<TicketLog, Long> {
    List<TicketLog> findByTicketIdOrderByTimestampDesc(Long ticketId);
}