package com.fixpoint.business.ticketparts.controller;

import com.fixpoint.business.ticketparts.dto.AddTicketPartDTO;
import com.fixpoint.business.ticketparts.dto.TicketPartDTO;
import com.fixpoint.business.ticketparts.service.TicketPartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/parts")
@RequiredArgsConstructor
public class TicketPartController {

    private final TicketPartService ticketPartService;

    @PostMapping
    public TicketPartDTO addPart(
            @PathVariable Long ticketId,
            @RequestBody @Valid AddTicketPartDTO dto
    ) {
        return ticketPartService.addPartToTicket(ticketId, dto);
    }

    @GetMapping
    public List<TicketPartDTO> getParts(@PathVariable Long ticketId) {
        return ticketPartService.getPartsForTicket(ticketId);
    }
}
