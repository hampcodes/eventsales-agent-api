package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.dto.request.TicketRequestDTO;
import com.hampcode.ticketsales.entity.Event;
import com.hampcode.ticketsales.entity.Participant;
import com.hampcode.ticketsales.entity.Ticket;
import com.hampcode.ticketsales.exception.EventSoldOutException;
import com.hampcode.ticketsales.exception.InvalidRequestException;
import com.hampcode.ticketsales.exception.ResourceNotFoundException;
import com.hampcode.ticketsales.dto.response.TicketResponseDTO;
import com.hampcode.ticketsales.mapper.TicketMapper;
import com.hampcode.ticketsales.repository.EventRepository;
import com.hampcode.ticketsales.repository.ParticipantRepository;
import com.hampcode.ticketsales.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    private static final Long EVENT_ID = 10L;
    private static final Long USER_ID = 5L;

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketService ticketService;

    @BeforeEach
    void authenticateAs() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(String.valueOf(USER_ID))
                .claim("role", "PARTICIPANT")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("purchaseTicket lanza EventSoldOutException si el evento no tiene cupo")
    void purchaseTicket_soldOutEvent_throwsEventSoldOutException() {
        Event soldOut = Event.builder()
                .id(EVENT_ID)
                .name("Concierto")
                .capacityMax(100)
                .date(LocalDate.now().plusDays(1))
                .availableCapacity(0)
                .basePrice(new BigDecimal("50.00"))
                .build();
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(soldOut));
        given(participantRepository.findByUser_Id(USER_ID)).willReturn(Optional.of(new Participant()));

        assertThatThrownBy(() -> ticketService.purchaseTicket(new TicketRequestDTO(EVENT_ID)))
                .isInstanceOf(EventSoldOutException.class)
                .hasMessage("El evento está agotado");

        assertThat(soldOut.getAvailableCapacity()).isZero();
        then(eventRepository).should().findByIdForUpdate(EVENT_ID);
        then(eventRepository).should(never()).save(any(Event.class));
        then(ticketRepository).should(never()).save(any(Ticket.class));
    }

    private Event event(LocalDate date, int availableCapacity) {
        return Event.builder()
                .id(EVENT_ID)
                .name("Concierto")
                .date(date)
                .capacityMax(100)
                .availableCapacity(availableCapacity)
                .basePrice(new BigDecimal("50.00"))
                .build();
    }

    @Test
    @DisplayName("purchaseTicket lanza InvalidRequestException si el evento ya pasó")
    void purchaseTicket_pastEvent_throwsInvalidRequestException() {
        Event past = event(LocalDate.now().minusDays(1), 10);
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(past));
        given(participantRepository.findByUser_Id(USER_ID)).willReturn(Optional.of(new Participant()));

        assertThatThrownBy(() -> ticketService.purchaseTicket(new TicketRequestDTO(EVENT_ID)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("No se pueden comprar entradas de un evento que ya pasó");

        assertThat(past.getAvailableCapacity()).isEqualTo(10);
        then(eventRepository).should().findByIdForUpdate(EVENT_ID);
        then(eventRepository).should(never()).save(any(Event.class));
        then(ticketRepository).should(never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("purchaseTicket responde 'ya pasó' y no 'agotado' si el evento pasado no tiene cupo")
    void purchaseTicket_pastAndSoldOutEvent_throwsInvalidRequestException() {
        Event past = event(LocalDate.now().minusDays(1), 0);
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(past));
        given(participantRepository.findByUser_Id(USER_ID)).willReturn(Optional.of(new Participant()));

        assertThatThrownBy(() -> ticketService.purchaseTicket(new TicketRequestDTO(EVENT_ID)))
                .isInstanceOf(InvalidRequestException.class);

        then(ticketRepository).should(never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("purchaseTicket crea la entrada PENDING y descuenta cupo si el evento es futuro")
    void purchaseTicket_futureEvent_createsPendingTicket() {
        Event future = event(LocalDate.now().plusDays(1), 10);
        Participant participant = new Participant();
        TicketResponseDTO response = mock(TicketResponseDTO.class);
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(future));
        given(participantRepository.findByUser_Id(USER_ID)).willReturn(Optional.of(participant));
        given(ticketRepository.save(any(Ticket.class))).willAnswer(inv -> inv.getArgument(0));
        given(ticketMapper.toResponseDTO(any(Ticket.class))).willReturn(response);

        TicketResponseDTO result = ticketService.purchaseTicket(new TicketRequestDTO(EVENT_ID));

        assertThat(result).isSameAs(response);
        assertThat(future.getAvailableCapacity()).isEqualTo(9);
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        then(ticketRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
        then(eventRepository).should().save(future);
    }

    @Test
    @DisplayName("purchaseTicket lanza ResourceNotFoundException si el evento no existe")
    void purchaseTicket_unknownEvent_throwsResourceNotFoundException() {
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.purchaseTicket(new TicketRequestDTO(EVENT_ID)))
                .isInstanceOf(ResourceNotFoundException.class);

        then(ticketRepository).should(never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("purchaseTicket lanza ResourceNotFoundException si el participante no existe")
    void purchaseTicket_unknownParticipant_throwsResourceNotFoundException() {
        Event future = event(LocalDate.now().plusDays(1), 10);
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(future));
        given(participantRepository.findByUser_Id(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.purchaseTicket(new TicketRequestDTO(EVENT_ID)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(future.getAvailableCapacity()).isEqualTo(10);
        then(eventRepository).should(never()).save(any(Event.class));
        then(ticketRepository).should(never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("purchaseTicket responde 404 y no 'ya pasó' si el evento es pasado y el participante no existe")
    void purchaseTicket_pastEventAndUnknownParticipant_throwsResourceNotFoundException() {
        Event past = event(LocalDate.now().minusDays(1), 10);
        given(eventRepository.findByIdForUpdate(EVENT_ID)).willReturn(Optional.of(past));
        given(participantRepository.findByUser_Id(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.purchaseTicket(new TicketRequestDTO(EVENT_ID)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(past.getAvailableCapacity()).isEqualTo(10);
        then(eventRepository).should(never()).save(any(Event.class));
        then(ticketRepository).should(never()).save(any(Ticket.class));
    }
}
