package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.dto.request.EventRequestDTO;
import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import com.hampcode.ticketsales.entity.Category;
import com.hampcode.ticketsales.entity.Event;
import com.hampcode.ticketsales.entity.Organizer;
import com.hampcode.ticketsales.entity.User;
import com.hampcode.ticketsales.exception.DuplicateResourceException;
import com.hampcode.ticketsales.exception.InvalidRequestException;
import com.hampcode.ticketsales.exception.ResourceNotFoundException;
import com.hampcode.ticketsales.mapper.EventMapper;
import com.hampcode.ticketsales.mapper.TicketMapper;
import com.hampcode.ticketsales.repository.CategoryRepository;
import com.hampcode.ticketsales.repository.EventRepository;
import com.hampcode.ticketsales.repository.OrganizerRepository;
import com.hampcode.ticketsales.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    private static final Long USER_ID = 5L;
    private static final Long OTHER_USER_ID = 6L;
    private static final Long EVENT_ID = 10L;
    private static final Long CATEGORY_ID = 2L;
    private static final LocalDate DATE = LocalDate.now().plusDays(30);

    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private OrganizerRepository organizerRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private EventService eventService;

    @BeforeEach
    void authenticateAs() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(String.valueOf(USER_ID))
                .claim("role", "ORGANIZER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static EventRequestDTO request(Integer capacityMax, Long categoryId) {
        return new EventRequestDTO("Festival Jazz", DATE, "Lima", capacityMax,
                new BigDecimal("80.00"), "https://img.example.com/jazz.jpg", categoryId);
    }

    private static Organizer organizerOf(Long userId) {
        return Organizer.builder().id(1L).name("Org").user(User.builder().id(userId).build()).build();
    }

    private static Event existingEvent(Long ownerUserId) {
        return Event.builder().id(EVENT_ID).organizer(organizerOf(ownerUserId)).name("Viejo")
                .date(DATE.plusDays(1)).city("Cusco").capacityMax(100).availableCapacity(70)
                .basePrice(BigDecimal.TEN).imageUrl("x").build();
    }

    @Test
    @DisplayName("createEvent con categoryId valido asigna la categoria al evento guardado")
    void createEvent_validCategory_savesEventWithCategory() {
        Category category = Category.builder().id(CATEGORY_ID).name("Música").build();
        Event mapped = Event.builder().name("Festival Jazz").build();
        given(organizerRepository.findByUser_Id(USER_ID)).willReturn(Optional.of(organizerOf(USER_ID)));
        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.of(category));
        given(eventRepository.findByDateAndCity(DATE, "Lima")).willReturn(Optional.empty());
        given(eventMapper.toEntity(any(EventRequestDTO.class))).willReturn(mapped);
        given(eventRepository.save(mapped)).willReturn(mapped);

        eventService.createEvent(request(100, CATEGORY_ID));

        assertThat(mapped.getCategory()).isSameAs(category);
        then(eventRepository).should().save(mapped);
    }

    @Test
    @DisplayName("createEvent sin categoryId guarda sin categoria y no consulta CategoryRepository")
    void createEvent_nullCategory_savesWithoutCategory() {
        Event mapped = Event.builder().name("Festival Jazz").build();
        given(organizerRepository.findByUser_Id(USER_ID)).willReturn(Optional.of(organizerOf(USER_ID)));
        given(eventRepository.findByDateAndCity(DATE, "Lima")).willReturn(Optional.empty());
        given(eventMapper.toEntity(any(EventRequestDTO.class))).willReturn(mapped);
        given(eventRepository.save(mapped)).willReturn(mapped);

        eventService.createEvent(request(100, null));

        assertThat(mapped.getCategory()).isNull();
        then(categoryRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("createEvent con categoryId inexistente lanza ResourceNotFoundException y no guarda")
    void createEvent_missingCategory_throwsResourceNotFoundException() {
        given(organizerRepository.findByUser_Id(USER_ID)).willReturn(Optional.of(organizerOf(USER_ID)));
        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(request(100, CATEGORY_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoría no encontrada");
        then(eventRepository).should(never()).save(any(Event.class));
    }

    @Test
    @DisplayName("updateEvent del organizador dueño actualiza campos, categoria y cupo disponible")
    void updateEvent_owner_updatesEventAndRecalculatesAvailableCapacity() {
        Event event = existingEvent(USER_ID);
        Category category = Category.builder().id(CATEGORY_ID).name("Música").build();
        EventResponseDTO dto = new EventResponseDTO(EVENT_ID, "Org", "Festival Jazz", DATE, "Lima",
                150, 120, new BigDecimal("80.00"), "https://img.example.com/jazz.jpg", CATEGORY_ID, "Música");
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(event));
        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.of(category));
        given(eventRepository.findByDateAndCity(DATE, "Lima")).willReturn(Optional.of(event));
        given(eventRepository.save(event)).willReturn(event);
        given(eventMapper.toResponseDTO(event)).willReturn(dto);

        EventResponseDTO result = eventService.updateEvent(EVENT_ID, request(150, CATEGORY_ID));

        assertThat(result).isEqualTo(dto);
        assertThat(event.getName()).isEqualTo("Festival Jazz");
        assertThat(event.getCity()).isEqualTo("Lima");
        assertThat(event.getCapacityMax()).isEqualTo(150);
        assertThat(event.getAvailableCapacity()).isEqualTo(120);
        assertThat(event.getCategory()).isSameAs(category);
    }

    @Test
    @DisplayName("updateEvent con categoryId null deja el evento sin categoria")
    void updateEvent_nullCategory_clearsCategory() {
        Event event = existingEvent(USER_ID);
        event.setCategory(Category.builder().id(CATEGORY_ID).name("Música").build());
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(event));
        given(eventRepository.findByDateAndCity(DATE, "Lima")).willReturn(Optional.empty());
        given(eventRepository.save(event)).willReturn(event);

        eventService.updateEvent(EVENT_ID, request(100, null));

        assertThat(event.getCategory()).isNull();
        then(categoryRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("updateEvent de un evento de otro organizador lanza AccessDeniedException y no guarda")
    void updateEvent_notOwner_throwsAccessDeniedException() {
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(existingEvent(OTHER_USER_ID)));

        assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, request(100, null)))
                .isInstanceOf(AccessDeniedException.class);
        then(eventRepository).should(never()).save(any(Event.class));
    }

    @Test
    @DisplayName("updateEvent de un evento inexistente lanza ResourceNotFoundException")
    void updateEvent_missingEvent_throwsResourceNotFoundException() {
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, request(100, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Evento no encontrado");
    }

    @Test
    @DisplayName("updateEvent con categoryId inexistente lanza ResourceNotFoundException y no guarda")
    void updateEvent_missingCategory_throwsResourceNotFoundException() {
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(existingEvent(USER_ID)));
        given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, request(100, CATEGORY_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoría no encontrada");
        then(eventRepository).should(never()).save(any(Event.class));
    }

    @Test
    @DisplayName("updateEvent con fecha y ciudad de otro evento lanza DuplicateResourceException")
    void updateEvent_duplicateDateAndCity_throwsDuplicateResourceException() {
        Event other = Event.builder().id(99L).build();
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(existingEvent(USER_ID)));
        given(eventRepository.findByDateAndCity(DATE, "Lima")).willReturn(Optional.of(other));

        assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, request(100, null)))
                .isInstanceOf(DuplicateResourceException.class);
        then(eventRepository).should(never()).save(any(Event.class));
    }

    @Test
    @DisplayName("updateEvent con capacidad menor a las entradas vendidas lanza InvalidRequestException")
    void updateEvent_capacityBelowSold_throwsInvalidRequestException() {
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(existingEvent(USER_ID)));

        assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, request(29, null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("30");
        then(eventRepository).should(never()).save(any(Event.class));
    }

    @Test
    @DisplayName("updateEvent con capacidad igual a las entradas vendidas deja el cupo disponible en 0")
    void updateEvent_capacityEqualsSold_availableCapacityZero() {
        Event event = existingEvent(USER_ID);
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(event));
        given(eventRepository.findByDateAndCity(DATE, "Lima")).willReturn(Optional.empty());
        given(eventRepository.save(event)).willReturn(event);

        eventService.updateEvent(EVENT_ID, request(30, null));

        assertThat(event.getCapacityMax()).isEqualTo(30);
        assertThat(event.getAvailableCapacity()).isZero();
    }
}
