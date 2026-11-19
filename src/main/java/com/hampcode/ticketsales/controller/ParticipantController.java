package com.hampcode.ticketsales.controller;

import com.hampcode.ticketsales.dto.request.ParticipantUpdateRequestDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.dto.response.ParticipantResponseDTO;
import com.hampcode.ticketsales.dto.response.TicketResponseDTO;
import com.hampcode.ticketsales.service.ParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/participants")
@RequiredArgsConstructor
public class ParticipantController {
    private final ParticipantService participantService;

    @PreAuthorize("hasRole('PARTICIPANT')")
    @PatchMapping("/{participantId}")
    public ResponseEntity<ParticipantResponseDTO> updateContact(
            @PathVariable Long participantId,
            @Valid @RequestBody ParticipantUpdateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.OK).body(participantService.updateContactInfo(participantId, request));
    }

    @PreAuthorize("hasRole('PARTICIPANT')")
    @GetMapping("/{participantId}/tickets")
    public ResponseEntity<PageResponseDTO<TicketResponseDTO>> listTickets(
            @PathVariable Long participantId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(participantService.listTicketsByParticipant(participantId, pageable));
    }

    @PreAuthorize("hasRole('PARTICIPANT')")
    @GetMapping("/{participantId}/events/{eventId}/tickets")
    public ResponseEntity<List<TicketResponseDTO>> listTicketsForEvent(
            @PathVariable Long participantId, @PathVariable Long eventId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(participantService.listTicketsByParticipantAndEvent(participantId, eventId));
    }
}
