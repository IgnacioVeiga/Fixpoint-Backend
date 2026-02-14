package com.fixpoint.business.ticketlogs.controller;

import com.fixpoint.business.ticketlogs.dto.CreateTicketLogDTO;
import com.fixpoint.business.ticketlogs.dto.TicketLogDTO;
import com.fixpoint.business.ticketlogs.service.TicketLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/logs")
@RequiredArgsConstructor
public class TicketLogController {

    private final TicketLogService ticketLogService;

    @PostMapping
    public TicketLogDTO addLog(
            @PathVariable Long ticketId,
            @RequestBody @Valid CreateTicketLogDTO dto
    ) {
        return ticketLogService.createLog(ticketId, dto);
    }

    @GetMapping
    public List<TicketLogDTO> getLogs(@PathVariable Long ticketId) {
        return ticketLogService.getLogsByTicketId(ticketId);
    }
}
