package com.hampcode.ticketsales.dto.response;

import java.util.List;

public record HomeResponseDTO(
        List<CategoryCountResponseDTO> categories,
        List<EventResponseDTO> upcomingEvents) {}
