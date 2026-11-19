package com.hampcode.ticketsales.dto.response;

import java.math.BigDecimal;

public record TotalSpentResponseDTO(String participantName, BigDecimal totalSpent) {}
