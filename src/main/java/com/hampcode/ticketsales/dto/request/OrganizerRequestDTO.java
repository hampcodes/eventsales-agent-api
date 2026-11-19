package com.hampcode.ticketsales.dto.request;

import jakarta.validation.constraints.*;

public record OrganizerRequestDTO(
        @NotBlank String name,
        @NotBlank String documentId,
        @NotBlank @Email String email,
        String phone,
        String website) {}
