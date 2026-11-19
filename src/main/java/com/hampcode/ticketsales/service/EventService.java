package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.config.CurrentUser;
import com.hampcode.ticketsales.dto.request.EventRequestDTO;
import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.dto.response.TicketResponseDTO;
import com.hampcode.ticketsales.entity.Category;
import com.hampcode.ticketsales.entity.Event;
import com.hampcode.ticketsales.entity.Organizer;
import com.hampcode.ticketsales.entity.Ticket;
import com.hampcode.ticketsales.exception.DuplicateResourceException;
import com.hampcode.ticketsales.exception.InvalidRequestException;
import com.hampcode.ticketsales.exception.ResourceNotFoundException;
import com.hampcode.ticketsales.mapper.EventMapper;
import com.hampcode.ticketsales.mapper.TicketMapper;
import com.hampcode.ticketsales.repository.CategoryRepository;
import com.hampcode.ticketsales.repository.EventRepository;
import com.hampcode.ticketsales.repository.OrganizerRepository;
import com.hampcode.ticketsales.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final OrganizerRepository organizerRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final TicketMapper ticketMapper;

    @Transactional
    public EventResponseDTO createEvent(EventRequestDTO request) {
        Organizer organizer = organizerRepository.findByUser_Id(CurrentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizador no encontrado"));
        Category category = resolveCategory(request.categoryId());
        if (eventRepository.findByDateAndCity(request.date(), request.city()).isPresent()) {
            throw new DuplicateResourceException("El evento ya existe para esa fecha y ciudad");
        }
        Event event = eventMapper.toEntity(request);

        event.setOrganizer(organizer);
        event.setCategory(category);

        return eventMapper.toResponseDTO(eventRepository.save(event));
    }

    @Transactional
    public EventResponseDTO updateEvent(Long eventId, EventRequestDTO request) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
        if (!event.getOrganizer().getUser().getId().equals(CurrentUser.getUserId())) {
            throw new AccessDeniedException("No es tu evento");
        }
        Category category = resolveCategory(request.categoryId());

        int sold = event.getCapacityMax() - event.getAvailableCapacity();
        if (request.capacityMax() < sold) {
            throw new InvalidRequestException(
                    "La capacidad no puede ser menor a las entradas ya vendidas (" + sold + ")");
        }

        boolean duplicated = eventRepository.findByDateAndCity(request.date(), request.city())
                .filter(other -> !other.getId().equals(eventId))
                .isPresent();
        if (duplicated) {
            throw new DuplicateResourceException("El evento ya existe para esa fecha y ciudad");
        }
        event.setName(request.name());
        event.setDate(request.date());
        event.setCity(request.city());
        event.setCapacityMax(request.capacityMax());
        event.setAvailableCapacity(request.capacityMax() - sold);
        event.setBasePrice(request.basePrice());
        event.setImageUrl(request.imageUrl());
        event.setCategory(category);
        return eventMapper.toResponseDTO(eventRepository.save(event));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }

    public EventResponseDTO getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
        return eventMapper.toResponseDTO(event);
    }

    public PageResponseDTO<EventResponseDTO> listEvents(Pageable pageable) {
        return toPage(eventRepository.findAll(pageable));
    }

    public List<EventResponseDTO> searchTop(String name, String city) {
        return searchEvents(name, city, null, null, null, null, PageRequest.of(0, 5)).content();
    }

    public List<EventResponseDTO> searchMyEvents(String name, String city) {
        Organizer organizer = organizerRepository.findByUser_Id(CurrentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizador no encontrado"));
        return searchEvents(name, city, null, null, organizer.getId(), null,
                PageRequest.of(0, 10, Sort.by("date"))).content();
    }

    public PageResponseDTO<EventResponseDTO> searchEvents(String name, String city, LocalDate startDate,
                                                          LocalDate endDate, Long organizerId, Long categoryId,
                                                          Pageable pageable) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidRequestException("El rango de fechas es inválido: startDate es posterior a endDate");
        }
        Specification<Event> spec = (root, query, cb) -> cb.conjunction();
        if (name != null && !name.isBlank()) {
            String texto = withoutAccents(name.trim()).toLowerCase(Locale.ROOT);
            var palabras = java.util.Arrays.stream(texto.split("\\s+")).filter(w -> w.length() > 2).toList();
            if (palabras.isEmpty()) palabras = List.of(texto);
            for (String palabra : palabras) {
                String pattern = "%" + escapeLike(palabra) + "%";
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\'));
            }
        }
        if (city != null && !city.isBlank()) {
            String value = withoutAccents(city.trim()).toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("city")), value));
        }
        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("date"), startDate));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("date"), endDate));
        }
        if (organizerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("organizer").get("id"), organizerId));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        return toPage(eventRepository.findAll(spec, pageable));
    }

    private static String withoutAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public PageResponseDTO<TicketResponseDTO> listTicketsByEvent(Long eventId, Pageable pageable) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

        if (!event.getOrganizer().getUser().getId().equals(CurrentUser.getUserId())) {
            throw new AccessDeniedException("No es tu evento");
        }
        Page<Ticket> page = ticketRepository.findByEventId(eventId, pageable);
        return new PageResponseDTO<>(
                page.getContent().stream().map(ticketMapper::toResponseDTO).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }

    private PageResponseDTO<EventResponseDTO> toPage(Page<Event> page) {
        return new PageResponseDTO<>(
                page.getContent().stream().map(eventMapper::toResponseDTO).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }
}
