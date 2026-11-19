package com.hampcode.ticketsales.dto.response;

public record LoginResponseDTO(
        String accessToken, String tokenType, long expiresIn, String role, Long profileId) {}
