package com.fixpoint.business.attachments.repository;

import com.fixpoint.business.attachments.entity.Attachment;
import com.fixpoint.business.tickets.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTicket(Ticket ticket);
    boolean existsByTicketId(Long ticketId);

    @Query("select coalesce(sum(a.fileSizeBytes), 0) from Attachment a")
    long sumFileSizeBytes();
}
