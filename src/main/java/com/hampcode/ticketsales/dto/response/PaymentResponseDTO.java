package com.hampcode.ticketsales.dto.response;

import com.hampcode.ticketsales.entity.PaymentMethod;
import java.math.BigDecimal;

public record PaymentResponseDTO(
        Long id, Long ticketId, BigDecimal amount, PaymentMethod method, String status) {}
