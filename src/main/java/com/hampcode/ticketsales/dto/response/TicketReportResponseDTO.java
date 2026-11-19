package com.hampcode.ticketsales.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record TicketReportResponseDTO(
        String eventName, Map<String, Long> ticketsByStatus, BigDecimal totalRevenue) {}
