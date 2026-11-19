package com.hampcode.ticketsales.agent.app.dto;

import java.util.List;

public record ChartData(String type, String title, String summary, List<String> labels, List<Number> values) {}
