package com.hampcode.ticketsales.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EventRequestDTO(
        @NotBlank String name,
        @NotNull @Future LocalDate date,
        @NotBlank String city,
        @NotNull @Positive Integer capacityMax,
        @NotNull @Positive BigDecimal basePrice,
        @NotBlank String imageUrl,
        Long categoryId) {}
