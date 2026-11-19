package com.hampcode.ticketsales.agent.app.dto;

public record VoiceResponse(String transcript, AgentResponse response, byte[] audio) {}
