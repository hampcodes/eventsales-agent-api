package com.hampcode.ticketsales.agent.core.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(@NotBlank @Size(max = 1000) String message, @Size(max = 36) String conversationId) {}
