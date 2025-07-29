package com.fixpoint.ticketlogs.controller;

import com.fixpoint.ticketlogs.dto.CreateTicketLogDTO;
import com.fixpoint.ticketlogs.dto.TicketLogDTO;
import com.fixpoint.ticketlogs.service.TicketLogService;
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
        return ticketLogService.createLog(new CreateTicketLogDTO(
                ticketId,
                dto.description(),
                dto.author()
        ));
    }

    @GetMapping
    public List<TicketLogDTO> getLogs(@PathVariable Long ticketId) {
        return ticketLogService.getLogsByTicketId(ticketId);
    }
}