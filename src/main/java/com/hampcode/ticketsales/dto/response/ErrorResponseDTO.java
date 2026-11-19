package com.hampcode.ticketsales.dto.response;

import java.time.LocalDateTime;

public record ErrorResponseDTO(LocalDateTime timestamp, int status, String error, String message) {}
