package com.hampcode.ticketsales.dto.request;

import com.hampcode.ticketsales.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PaymentRequestDTO(
        @NotNull Long ticketId,
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod method) {}
