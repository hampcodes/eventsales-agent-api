package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.dto.response.CategoryCountResponseDTO;
import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import com.hampcode.ticketsales.dto.response.HomeResponseDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EventService eventService;
    @InjectMocks
    private HomeService homeService;

    @Test
    @DisplayName("Devuelve las categorías con su conteo y los 3 próximos eventos desde hoy")
    void getHome_returnsCategoriesAndUpcomingEvents() {
        LocalDate today = LocalDate.now();
        CategoryCountResponseDTO musica = new CategoryCountResponseDTO(1L, "Musica", 3);
        EventResponseDTO evento = new EventResponseDTO(
                10L, "Eventos Lima SAC", "Festival", today.plusDays(5), "Lima",
                100, 80, new BigDecimal("120.00"), "https://img/1.png", 1L, "Musica");
        given(categoryRepository.findAllWithUpcomingEventCount(today)).willReturn(List.of(musica));
        given(eventService.searchEvents(any(), any(), eq(today), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageResponseDTO<>(List.of(evento), 0, 3, 1, 1, true));

        HomeResponseDTO home = homeService.getHome();

        assertThat(home.categories()).containsExactly(musica);
        assertThat(home.upcomingEvents()).containsExactly(evento);
    }

    @Test
    @DisplayName("Pide los eventos desde hoy, ordenados por fecha ascendente y máximo 3")
    void getHome_asksForThreeClosestEvents() {
        LocalDate today = LocalDate.now();
        given(categoryRepository.findAllWithUpcomingEventCount(today)).willReturn(List.of());
        given(eventService.searchEvents(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageResponseDTO<>(List.of(), 0, 3, 0, 0, true));

        homeService.getHome();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(eventService).should().searchEvents(eq(null), eq(null), eq(today), eq(null), eq(null), eq(null), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(3);
        assertThat(pageable.getValue().getSort().getOrderFor("date").isAscending()).isTrue();
    }
}
