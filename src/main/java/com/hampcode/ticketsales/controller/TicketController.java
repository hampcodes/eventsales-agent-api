package com.hampcode.ticketsales.controller;

import com.hampcode.ticketsales.dto.request.TicketRequestDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.dto.response.TicketDetailDTO;
import com.hampcode.ticketsales.dto.response.TicketResponseDTO;
import com.hampcode.ticketsales.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PreAuthorize("hasRole('PARTICIPANT')")
    @PostMapping
    public ResponseEntity<TicketResponseDTO> purchase(@Valid @RequestBody TicketRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.purchaseTicket(request));
    }

    @PreAuthorize("hasRole('PARTICIPANT')")
    @GetMapping("/mine")
    public ResponseEntity<PageResponseDTO<TicketResponseDTO>> mine(
            @RequestParam(defaultValue = "") String name,
            Pageable pageable) {
        return ResponseEntity.ok(ticketService.myTickets(name, pageable));
    }

    @PreAuthorize("hasRole('PARTICIPANT')")
    @GetMapping("/{id}")
    public ResponseEntity<TicketDetailDTO> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.myTicketDetail(id));
    }

    @PreAuthorize("hasRole('PARTICIPANT')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TicketResponseDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.cancelTicket(id));
    }
}
