package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.dto.response.CategoryCountResponseDTO;
import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import com.hampcode.ticketsales.dto.response.HomeResponseDTO;
import com.hampcode.ticketsales.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {
    private static final int UPCOMING_LIMIT = 3;

    private final CategoryRepository categoryRepository;
    private final EventService eventService;

    public HomeResponseDTO getHome() {
        LocalDate today = LocalDate.now();

        List<CategoryCountResponseDTO> categories = categoryRepository.findAllWithUpcomingEventCount(today);

        List<EventResponseDTO> upcomingEvents = eventService
                .searchEvents(null, null, today, null, null, null,
                        PageRequest.of(0, UPCOMING_LIMIT, Sort.by("date").ascending()))
                .content();

        return new HomeResponseDTO(categories, upcomingEvents);
    }
}
