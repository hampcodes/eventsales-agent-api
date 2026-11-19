package com.hampcode.ticketsales.controller;

import com.hampcode.ticketsales.dto.request.EventRequestDTO;
import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.dto.response.TicketResponseDTO;
import com.hampcode.ticketsales.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping
    public ResponseEntity<EventResponseDTO> createEvent(@Valid @RequestBody EventRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponseDTO> updateEvent(@PathVariable Long eventId,
                                                        @Valid @RequestBody EventRequestDTO request) {
        return ResponseEntity.status(HttpStatus.OK).body(eventService.updateEvent(eventId, request));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponseDTO> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.status(HttpStatus.OK).body(eventService.getEventById(eventId));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<EventResponseDTO>> listEvents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable) {
        PageResponseDTO<EventResponseDTO> body =
                eventService.searchEvents(name, city, startDate, endDate, organizerId, categoryId, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/{eventId}/tickets")
    public ResponseEntity<PageResponseDTO<TicketResponseDTO>> listTickets(@PathVariable Long eventId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(eventService.listTicketsByEvent(eventId, pageable));
    }
}
