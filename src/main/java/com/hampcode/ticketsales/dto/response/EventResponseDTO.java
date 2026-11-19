package com.hampcode.ticketsales.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EventResponseDTO(
        Long id, String organizerName,
        String name, LocalDate date, String city,
        Integer capacityMax, Integer availableCapacity,
        BigDecimal basePrice, String imageUrl,
        Long categoryId, String categoryName) {}
