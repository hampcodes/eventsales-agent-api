package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.config.CurrentUser;
import com.hampcode.ticketsales.dto.request.TicketRequestDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.dto.response.TicketDetailDTO;
import com.hampcode.ticketsales.dto.response.TicketResponseDTO;
import com.hampcode.ticketsales.entity.Event;
import com.hampcode.ticketsales.entity.Participant;
import com.hampcode.ticketsales.entity.Ticket;
import com.hampcode.ticketsales.exception.EventSoldOutException;
import com.hampcode.ticketsales.exception.InvalidRequestException;
import com.hampcode.ticketsales.exception.ResourceNotFoundException;
import com.hampcode.ticketsales.mapper.TicketMapper;
import com.hampcode.ticketsales.repository.EventRepository;
import com.hampcode.ticketsales.repository.ParticipantRepository;
import com.hampcode.ticketsales.repository.PaymentRepository;
import com.hampcode.ticketsales.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final TicketMapper ticketMapper;

    @Transactional
    public TicketResponseDTO purchaseTicket(TicketRequestDTO request) {
        Event event = eventRepository.findByIdForUpdate(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

        Participant participant = participantRepository.findByUser_Id(CurrentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));

        if (event.getDate().isBefore(LocalDate.now())) {
            throw new InvalidRequestException("No se pueden comprar entradas de un evento que ya pasó");
        }
        if (event.getAvailableCapacity() <= 0) {
            throw new EventSoldOutException("El evento está agotado");
        }
        event.setAvailableCapacity(event.getAvailableCapacity() - 1);
        eventRepository.save(event);

        Ticket ticket = Ticket.builder()
                .event(event)
                .participant(participant)
                .pricePaid(event.getBasePrice())
                .status("PENDING")
                .build();
        return ticketMapper.toResponseDTO(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponseDTO cancelTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada no encontrada"));
        if (!ticket.getParticipant().getUser().getId().equals(CurrentUser.getUserId())) {
            throw new AccessDeniedException("No es tu entrada");
        }
        if (!"PENDING".equals(ticket.getStatus())) {
            throw new InvalidRequestException("Solo se pueden anular entradas pendientes de pago");
        }
        Event event = eventRepository.findByIdForUpdate(ticket.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
        event.setAvailableCapacity(event.getAvailableCapacity() + 1);
        eventRepository.save(event);

        ticket.setStatus("CANCELLED");
        return ticketMapper.toResponseDTO(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<TicketResponseDTO> myTickets(String name, Pageable pageable) {
        Participant participant = participantRepository.findByUser_Id(CurrentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));
        String texto = name == null ? "" : name.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");

        var page = ticketRepository.searchMine(participant.getId(), texto,
                PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 50)));
        return new PageResponseDTO<>(page.getContent().stream().map(ticketMapper::toResponseDTO).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public TicketDetailDTO myTicketDetail(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada no encontrada"));
        if (!ticket.getParticipant().getUser().getId().equals(CurrentUser.getUserId())) {
            throw new AccessDeniedException("No es tu entrada");
        }
        Event event = ticket.getEvent();
        var payment = paymentRepository.findByTicketId(ticket.getId()).orElse(null);
        return new TicketDetailDTO(ticket.getId(), event.getId(), event.getName(), event.getDate(), event.getCity(),
                ticket.getPricePaid(), ticket.getStatus(),
                payment == null ? null : payment.getMethod().name(),
                payment == null ? null : payment.getStatus(),
                payment == null ? null : payment.getAmount());
    }

    public Long findPendingTicketId(Long eventId) {
        Participant participant = participantRepository.findByUser_Id(CurrentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));
        return ticketRepository.findByParticipantIdAndEventId(participant.getId(), eventId).stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .max(Comparator.comparing(Ticket::getId))
                .map(Ticket::getId)
                .orElse(null);
    }
}
