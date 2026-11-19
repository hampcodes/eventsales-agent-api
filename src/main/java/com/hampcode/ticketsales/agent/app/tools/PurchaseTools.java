package com.hampcode.ticketsales.agent.app.tools;

import com.hampcode.ticketsales.agent.app.dto.PurchaseInfo;
import com.hampcode.ticketsales.agent.app.dto.ResponseData;
import com.hampcode.ticketsales.dto.request.PaymentRequestDTO;
import com.hampcode.ticketsales.dto.request.TicketRequestDTO;
import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import com.hampcode.ticketsales.entity.PaymentMethod;
import com.hampcode.ticketsales.service.EventService;
import com.hampcode.ticketsales.service.PaymentService;
import com.hampcode.ticketsales.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class PurchaseTools {

    private final EventService eventService;
    private final TicketService ticketService;
    private final PaymentService paymentService;
    private final Map<String, Supplier<PurchaseInfo>> pendingPurchases = new ConcurrentHashMap<>();

    @Tool(description = "Inicia la compra de UNA entrada (se paga al confirmar). NO la registra: deja la compra pendiente de confirmación.")
    public String buy(
            @ToolParam(description = "Nombre del evento de la conversación (nunca la frase \"quiero comprar\")", required = false) String name,
            @ToolParam(description = "Ciudad, solo si la menciona", required = false) String city,
            ToolContext context) {
        var events = eventService.searchTop(name, city);
        if (events.isEmpty()) return "No encontré ese evento.";
        if (events.size() > 1) return "Hay varios eventos, pregunta cuál quiere: " + events.stream().map(EventResponseDTO::name).toList();

        var event = events.get(0);
        pendingPurchases.put(userOf(context), () -> {
            var ticket = ticketService.purchaseTicket(new TicketRequestDTO(event.id()));
            paymentService.registerPayment(new PaymentRequestDTO(ticket.id(), ticket.pricePaid(), PaymentMethod.TARJETA));
            return new PurchaseInfo(ticket.id(), event.id(), ticket.eventName(), ticket.pricePaid(), "PAID");
        });
        ResponseData.of(context).setPurchase(new PurchaseInfo(null, event.id(), event.name(), event.basePrice(), "CONFIRMATION_REQUIRED"));
        return "Pendiente de confirmación: entrada para " + event.name() + " por S/ " + event.basePrice();
    }

    @Tool(description = "Ejecuta lo que el usuario YA confirmó en un mensaje anterior (sí, confirmo, dale). Sin parámetros.")
    public String confirm(ToolContext context) {
        return confirmPurchase(userOf(context), ResponseData.of(context));
    }

    public boolean hasPending(String userName) {
        return pendingPurchases.containsKey(userName);
    }

    public String confirmPurchase(String userName, ResponseData data) {
        if (data.getPurchase() != null) return "No había nada por confirmar.";
        var purchase = pendingPurchases.remove(userName);
        if (purchase == null) return "No había nada por confirmar.";
        data.setPurchase(purchase.get());
        return "Hecho.";
    }

    public void cancelPurchase(String userName) {
        pendingPurchases.remove(userName);
    }

    private static String userOf(ToolContext context) {
        return (String) context.getContext().get("user");
    }
}
