package com.hampcode.ticketsales.dto.response;

import java.math.BigDecimal;

public record TicketHistoryResponseDTO(
        String eventName, BigDecimal pricePaid, String status) {}
