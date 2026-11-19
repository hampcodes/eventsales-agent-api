package com.hampcode.ticketsales.dto.response;

public record ParticipantResponseDTO(
        Long id, String name, String documentId, String email, String phone) {}
