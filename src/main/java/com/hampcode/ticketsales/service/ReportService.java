package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.config.CurrentUser;
import com.hampcode.ticketsales.dto.response.DashboardDTO;
import com.hampcode.ticketsales.dto.response.DashboardEventDTO;
import com.hampcode.ticketsales.dto.response.EventSalesDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.dto.response.SalesReportRowDTO;
import com.hampcode.ticketsales.dto.response.TicketHistoryResponseDTO;
import com.hampcode.ticketsales.dto.response.TicketReportResponseDTO;
import com.hampcode.ticketsales.dto.response.TotalSpentResponseDTO;
import com.hampcode.ticketsales.entity.Event;
import com.hampcode.ticketsales.entity.Organizer;
import com.hampcode.ticketsales.entity.Participant;
import com.hampcode.ticketsales.exception.ResourceNotFoundException;
import com.hampcode.ticketsales.repository.EventRepository;
import com.hampcode.ticketsales.repository.OrganizerRepository;
import com.hampcode.ticketsales.repository.ParticipantRepository;
import com.hampcode.ticketsales.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final OrganizerRepository organizerRepository;

    public TicketReportResponseDTO getEventSalesReport(Long eventId) {
        Event event = findEvent(eventId);
        Map<String, Long> byStatus = emptyStatusMap();
        for (Object[] row : ticketRepository.countTicketsByStatusForEvent(eventId)) {
            byStatus.put((String) row[0], ((Number) row[1]).longValue());
        }
        BigDecimal revenue = BigDecimal.ZERO;
        List<Object[]> rows = ticketRepository.sumRevenueByEvent(eventId);
        if (!rows.isEmpty()) {
            revenue = toBigDecimal(rows.get(0)[1]);
        }
        return new TicketReportResponseDTO(event.getName(), byStatus, revenue);
    }

    public TicketReportResponseDTO getEventSalesReportFull(Long eventId) {
        Event event = findEvent(eventId);
        Map<String, Long> byStatus = emptyStatusMap();
        BigDecimal revenue = BigDecimal.ZERO;
        for (Object[] row : ticketRepository.reportTicketsByEvent(eventId)) {
            byStatus.put("PAID", ((Number) row[1]).longValue());
            revenue = toBigDecimal(row[2]);
        }
        return new TicketReportResponseDTO(event.getName(), byStatus, revenue);
    }

    public DashboardDTO getMyDashboard() {
        Organizer organizer = organizerRepository.findByUser_Id(CurrentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizador no encontrado"));
        List<DashboardEventDTO> events = ticketRepository.dashboardByOrganizer(organizer.getId()).stream()
                .map(r -> new DashboardEventDTO(((Number) r[0]).longValue(), (String) r[1],
                        LocalDate.parse(String.valueOf(r[2])), (String) r[3],
                        ((Number) r[4]).intValue(), ((Number) r[5]).longValue(), toBigDecimal(r[6])))
                .toList();

        long ticketsPaid = events.stream().mapToLong(DashboardEventDTO::ticketsPaid).sum();
        long capacity = events.stream().mapToLong(DashboardEventDTO::capacityMax).sum();
        BigDecimal revenue = events.stream().map(DashboardEventDTO::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        int upcoming = (int) events.stream().filter(e -> !e.date().isBefore(LocalDate.now())).count();
        int occupancy = capacity == 0 ? 0 : (int) Math.round(ticketsPaid * 100.0 / capacity);
        return new DashboardDTO(events.size(), upcoming, ticketsPaid, revenue, occupancy, events);
    }

    public List<SalesReportRowDTO> getMySalesReport() {
        Organizer organizer = organizerRepository.findByUser_Id(CurrentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizador no encontrado"));
        return ticketRepository.salesReportByOrganizer(organizer.getId()).stream()
                .map(r -> new SalesReportRowDTO((String) r[0], LocalDate.parse(String.valueOf(r[1])), (String) r[2],
                        ((Number) r[3]).intValue(), ((Number) r[4]).longValue(), ((Number) r[5]).longValue(),
                        ((Number) r[6]).longValue(), toBigDecimal(r[7])))
                .toList();
    }

    public List<EventSalesDTO> getMySalesByEvent() {
        Organizer organizer = organizerRepository.findByUser_Id(CurrentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizador no encontrado"));
        return ticketRepository.salesByEventForOrganizer(organizer.getId()).stream()
                .map(r -> new EventSalesDTO((String) r[0], ((Number) r[1]).longValue(), toBigDecimal(r[2])))
                .toList();
    }

    public PageResponseDTO<TicketHistoryResponseDTO> getPurchaseHistory(Long participantId, Pageable pageable) {
        findParticipant(participantId);
        Page<Object[]> page = ticketRepository.findTicketHistoryByParticipant(participantId, pageable);
        return new PageResponseDTO<>(
                page.getContent().stream()
                        .map(r -> new TicketHistoryResponseDTO((String) r[0], toBigDecimal(r[1]), (String) r[2]))
                        .toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }

    public List<TicketHistoryResponseDTO> getPurchaseHistoryFull(Long participantId) {
        findParticipant(participantId);
        return ticketRepository.reportTicketsByParticipant(participantId).stream()
                .map(r -> new TicketHistoryResponseDTO((String) r[0], toBigDecimal(r[2]), (String) r[1]))
                .toList();
    }

    public TotalSpentResponseDTO getTotalSpent(Long participantId) {
        Participant participant = findParticipant(participantId);
        List<Object[]> rows = ticketRepository.sumSpentByParticipant(participantId);
        BigDecimal total = rows.isEmpty() ? BigDecimal.ZERO : toBigDecimal(rows.get(0)[1]);
        return new TotalSpentResponseDTO(participant.getName(), total);
    }

    private Event findEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

        if (!event.getOrganizer().getUser().getId().equals(CurrentUser.getUserId())) {
            throw new AccessDeniedException("No es tu evento");
        }
        return event;
    }

    private Participant findParticipant(Long participantId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));

        if (!participant.getUser().getId().equals(CurrentUser.getUserId())) {
            throw new AccessDeniedException("No es tu información");
        }
        return participant;
    }

    private Map<String, Long> emptyStatusMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("PENDING", 0L);
        map.put("PAID", 0L);
        map.put("CANCELLED", 0L);
        return map;
    }

    private BigDecimal toBigDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
