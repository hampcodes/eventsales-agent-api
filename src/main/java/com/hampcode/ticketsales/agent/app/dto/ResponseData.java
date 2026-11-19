package com.hampcode.ticketsales.agent.app.dto;

import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import lombok.Getter;
import org.springframework.ai.chat.model.ToolContext;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ResponseData {

    public static final String CONTEXT_KEY = "data";
    private List<EventResponseDTO> events;
    private PurchaseInfo purchase;
    private ChartData chart;
    private FileInfo file;

    public static ResponseData of(ToolContext context) {
        return (ResponseData) context.getContext().get(CONTEXT_KEY);
    }

    public boolean hasData() {
        return events != null || purchase != null || chart != null || file != null;
    }

    public AgentResponse toResponse(String conversationId, String modelAnswer) {
        return new AgentResponse(conversationId, message(modelAnswer), purchase == null ? events : null, purchase, chart, file);
    }

    private String message(String modelAnswer) {
        if (purchase != null) {
            return "PAID".equals(purchase.status())
                    ? "Listo, compra realizada y pagada."
                    : "Dejé pendiente una entrada para %s por S/ %s. ¿Confirmas la compra?".formatted(purchase.eventName(), purchase.price());
        }
        if (events != null) {
            return events.isEmpty() ? "No encontré eventos con esos datos." : "Encontré %d evento(s).".formatted(events.size());
        }
        if (file != null) return "Tu reporte de ventas está listo para descargar.";
        if (chart != null) return chart.summary();
        return modelAnswer;
    }
}
