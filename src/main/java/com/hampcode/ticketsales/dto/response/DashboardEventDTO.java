package com.hampcode.ticketsales.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardEventDTO(
        Long id, String name, LocalDate date, String city,
        int capacityMax, long ticketsPaid, BigDecimal revenue) {}
