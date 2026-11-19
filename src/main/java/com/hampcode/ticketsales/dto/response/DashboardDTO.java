package com.hampcode.ticketsales.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDTO(
        int totalEvents, int upcomingEvents, long ticketsPaid, BigDecimal revenue,
        int occupancyPercent, List<DashboardEventDTO> events) {}
