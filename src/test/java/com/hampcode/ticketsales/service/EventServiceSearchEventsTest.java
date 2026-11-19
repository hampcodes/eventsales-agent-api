package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.entity.Event;
import com.hampcode.ticketsales.exception.InvalidRequestException;
import com.hampcode.ticketsales.mapper.EventMapper;
import com.hampcode.ticketsales.mapper.TicketMapper;
import com.hampcode.ticketsales.repository.CategoryRepository;
import com.hampcode.ticketsales.repository.EventRepository;
import com.hampcode.ticketsales.repository.OrganizerRepository;
import com.hampcode.ticketsales.repository.TicketRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventServiceSearchEventsTest {
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

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

    private EventService eventService() {
        return new EventService(eventRepository, ticketRepository, organizerRepository, categoryRepository, eventMapper, ticketMapper);
    }

    @SuppressWarnings("unchecked")
    private Specification<Event> captureSpec() {
        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        then(eventRepository).should().findAll(captor.capture(), eq(PAGEABLE));
        return captor.getValue();
    }

    private void givenEmptyPage() {
        given(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class CriteriaDouble {
        final Root<Event> root = org.mockito.Mockito.mock(Root.class);
        final CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        final CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        final Path path = org.mockito.Mockito.mock(Path.class);
        final Expression lowered = org.mockito.Mockito.mock(Expression.class);
        final Predicate predicate = org.mockito.Mockito.mock(Predicate.class);

        CriteriaDouble() {
            given(root.get(anyString())).willReturn(path);
            given(path.get(anyString())).willReturn(path);
            given(cb.lower(any(Expression.class))).willReturn(lowered);
            given(cb.conjunction()).willReturn(predicate);
            given(cb.and(any(Predicate.class), any(Predicate.class))).willReturn(predicate);
            given(cb.like(any(Expression.class), anyString(), anyChar())).willReturn(predicate);
            given(cb.equal(any(Expression.class), any(Object.class))).willReturn(predicate);
            given(cb.greaterThanOrEqualTo(any(Expression.class), any(Comparable.class))).willReturn(predicate);
            given(cb.lessThanOrEqualTo(any(Expression.class), any(Comparable.class))).willReturn(predicate);
        }

        void apply(Specification<Event> spec) {
            spec.toPredicate(root, query, cb);
        }
    }

    @Test
    @DisplayName("searchEvents lanza InvalidRequestException si startDate es posterior a endDate")
    void searchEvents_startAfterEnd_throwsInvalidRequestException() {
        assertThatThrownBy(() -> eventService().searchEvents(
                null, null, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), null, null, PAGEABLE))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("startDate es posterior a endDate");
        then(eventRepository).should(never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("searchEvents acepta startDate igual a endDate")
    void searchEvents_sameDay_isAccepted() {
        givenEmptyPage();
        LocalDate day = LocalDate.of(2026, 11, 15);

        PageResponseDTO<EventResponseDTO> result = eventService().searchEvents(null, null, day, day, null, null, PAGEABLE);

        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("searchEvents acepta solo startDate y aplica el filtro >=")
    void searchEvents_onlyStartDate_appliesGreaterThanOrEqual() {
        givenEmptyPage();
        LocalDate start = LocalDate.of(2026, 11, 1);

        eventService().searchEvents(null, null, start, null, null, null, PAGEABLE);

        CriteriaDouble c = new CriteriaDouble();
        c.apply(captureSpec());
        then(c.cb).should().greaterThanOrEqualTo(any(Expression.class), eq(start));
        then(c.cb).should(never()).lessThanOrEqualTo(any(Expression.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("searchEvents acepta solo endDate y aplica el filtro <=")
    void searchEvents_onlyEndDate_appliesLessThanOrEqual() {
        givenEmptyPage();
        LocalDate end = LocalDate.of(2026, 12, 31);

        eventService().searchEvents(null, null, null, end, null, null, PAGEABLE);

        CriteriaDouble c = new CriteriaDouble();
        c.apply(captureSpec());
        then(c.cb).should().lessThanOrEqualTo(any(Expression.class), eq(end));
        then(c.cb).should(never()).greaterThanOrEqualTo(any(Expression.class), any(LocalDate.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("searchEvents trata name en blanco o null como sin filtro")
    void searchEvents_blankName_noNameFilter(String name) {
        givenEmptyPage();

        eventService().searchEvents(name, null, null, null, null, null, PAGEABLE);

        CriteriaDouble c = new CriteriaDouble();
        c.apply(captureSpec());
        then(c.cb).should(never()).like(any(Expression.class), anyString(), anyChar());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("searchEvents trata city en blanco o null como sin filtro")
    void searchEvents_blankCity_noCityFilter(String city) {
        givenEmptyPage();

        eventService().searchEvents(null, city, null, null, null, null, PAGEABLE);

        CriteriaDouble c = new CriteriaDouble();
        c.apply(captureSpec());
        then(c.cb).should(never()).equal(any(Expression.class), any(Object.class));
    }

    @Test
    @DisplayName("searchEvents con todos los filtros consulta una vez y mapea a la pagina de respuesta")
    void searchEvents_allFilters_buildsQueryAndMapsPage() {
        Event event = Event.builder().id(1L).name("Rock Fest").city("Lima").build();
        EventResponseDTO dto = new EventResponseDTO(1L, "Org", "Rock Fest", LocalDate.of(2026, 11, 15),
                "Lima", 100, 40, new BigDecimal("50.00"), null, null, null);
        Page<Event> page = new PageImpl<>(List.of(event), PAGEABLE, 1);
        given(eventRepository.findAll(any(Specification.class), eq(PAGEABLE))).willReturn(page);
        given(eventMapper.toResponseDTO(event)).willReturn(dto);
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        PageResponseDTO<EventResponseDTO> result =
                eventService().searchEvents("Rock", "Lima", start, end, 3L, 7L, PAGEABLE);

        assertThat(result.content()).containsExactly(dto);
        assertThat(result.pageNumber()).isZero();
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.last()).isTrue();

        CriteriaDouble c = new CriteriaDouble();
        c.apply(captureSpec());
        then(c.cb).should().like(any(Expression.class), eq("%rock%"), eq('\\'));
        then(c.cb).should().greaterThanOrEqualTo(any(Expression.class), eq(start));
        then(c.cb).should().lessThanOrEqualTo(any(Expression.class), eq(end));

        then(c.cb).should().equal(any(Expression.class), eq("lima"));
        then(c.cb).should().equal(any(Expression.class), eq(3L));

        then(c.cb).should().equal(any(Expression.class), eq(7L));
    }

    @Test
    @DisplayName("searchEvents con solo categoryId aplica unicamente el filtro de categoria")
    void searchEvents_onlyCategoryId_appliesCategoryFilter() {
        givenEmptyPage();

        eventService().searchEvents(null, null, null, null, null, 7L, PAGEABLE);

        CriteriaDouble c = new CriteriaDouble();
        c.apply(captureSpec());
        then(c.root).should().get("category");
        then(c.cb).should().equal(any(Expression.class), eq(7L));
        then(c.cb).should(never()).like(any(Expression.class), anyString(), anyChar());
    }

    @Test
    @DisplayName("searchEvents sin categoryId no filtra por categoria")
    void searchEvents_noCategoryId_noCategoryFilter() {
        givenEmptyPage();

        eventService().searchEvents(null, null, null, null, null, null, PAGEABLE);

        CriteriaDouble c = new CriteriaDouble();
        c.apply(captureSpec());
        then(c.root).should(never()).get("category");
    }

    @Test
    @DisplayName("searchEvents escapa %, _ y \\ en name para tratarlos como texto literal")
    void searchEvents_nameWithWildcards_escapesLikePattern() {
        givenEmptyPage();

        eventService().searchEvents("100%_a\\b", null, null, null, null, null, PAGEABLE);

        CriteriaDouble c = new CriteriaDouble();
        c.apply(captureSpec());

        then(c.cb).should().like(any(Expression.class), eq("%100\\%\\_a\\\\b%"), eq('\\'));
    }
}
