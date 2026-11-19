package com.hampcode.ticketsales.dto.response;

import java.math.BigDecimal;

public record EventSalesDTO(String eventName, long ticketsPaid, BigDecimal revenue) {}
