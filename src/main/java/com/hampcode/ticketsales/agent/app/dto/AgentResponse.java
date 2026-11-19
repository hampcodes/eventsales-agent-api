package com.hampcode.ticketsales.agent.app.dto;

import com.hampcode.ticketsales.dto.response.EventResponseDTO;

import java.util.List;

public record AgentResponse(
        String conversationId,
        String message,
        List<EventResponseDTO> events,
        PurchaseInfo purchase,
        ChartData chart,
        FileInfo file) {
    public static AgentResponse onlyMessage(String conversationId, String message) {
        return new AgentResponse(conversationId, message, null, null, null, null);
    }
}
