package com.hampcode.ticketsales.dto.request;

import jakarta.validation.constraints.*;

public record ParticipantUpdateRequestDTO(@Email String email, String phone) {}
