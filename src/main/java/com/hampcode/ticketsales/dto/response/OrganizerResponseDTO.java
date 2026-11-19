package com.hampcode.ticketsales.dto.response;

public record OrganizerResponseDTO(
        Long id, String name, String documentId, String email, String phone, String website) {}
