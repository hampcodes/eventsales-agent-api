package com.hampcode.ticketsales.dto.request;

import jakarta.validation.constraints.*;

public record TicketRequestDTO(@NotNull Long eventId) {}
