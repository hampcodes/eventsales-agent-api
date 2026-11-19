package com.hampcode.ticketsales.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TicketDetailDTO(
        Long id, Long eventId, String eventName, LocalDate eventDate, String eventCity,
        BigDecimal pricePaid, String status,
        String paymentMethod, String paymentStatus, BigDecimal paymentAmount) {}
