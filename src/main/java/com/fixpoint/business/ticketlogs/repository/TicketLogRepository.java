package com.fixpoint.business.ticketlogs.repository;

import com.fixpoint.business.ticketlogs.entity.TicketLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketLogRepository extends JpaRepository<TicketLog, Long> {
    List<TicketLog> findByTicketIdOrderByTimestampDesc(Long ticketId);
    boolean existsByTicketId(Long ticketId);
}
