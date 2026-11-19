package com.hampcode.ticketsales.agent.app.tools;

import com.hampcode.ticketsales.agent.app.dto.ResponseData;
import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import com.hampcode.ticketsales.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventTools {

    private final EventService eventService;

    @Tool(description = "Busca eventos por ciudad o parte del nombre. Sin filtros devuelve los próximos. Úsala también para cupos y precios.")
    public List<EventResponseDTO> searchEvents(
            @ToolParam(description = "Ciudad, solo si la menciona", required = false) String city,
            @ToolParam(description = "Nombre del evento tal como lo escribió el usuario", required = false) String name,
            ToolContext context) {
        var events = eventService.searchTop(name, city);
        ResponseData.of(context).setEvents(events);
        return events;
    }
}
