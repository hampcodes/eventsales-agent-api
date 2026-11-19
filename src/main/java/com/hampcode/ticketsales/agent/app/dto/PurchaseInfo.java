package com.hampcode.ticketsales.agent.app.dto;

import java.math.BigDecimal;

public record PurchaseInfo(Long ticketId, Long eventId, String eventName, BigDecimal price, String status) {}
