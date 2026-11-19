package com.hampcode.ticketsales.agent.core;

public record AgentDefinition(String name, String description, String requiredRole, boolean usesPdfs, Object... tools) {}
