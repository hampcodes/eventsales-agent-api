package com.hampcode.ticketsales.dto.response;

import java.math.BigDecimal;

public record TicketResponseDTO(
        Long id, String eventName, String participantName,
        BigDecimal pricePaid, String status) {}
