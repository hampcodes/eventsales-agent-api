package com.hampcode.ticketsales.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesReportRowDTO(
        String eventName, LocalDate date, String city, int capacityMax,
        long ticketsPaid, long ticketsPending, long ticketsCancelled, BigDecimal revenue) {}
