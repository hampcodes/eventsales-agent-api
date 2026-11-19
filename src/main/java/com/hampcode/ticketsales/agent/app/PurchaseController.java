package com.hampcode.ticketsales.agent.app;

import com.hampcode.ticketsales.agent.app.dto.AgentResponse;
import com.hampcode.ticketsales.agent.app.dto.ResponseData;
import com.hampcode.ticketsales.agent.app.tools.PurchaseTools;
import com.hampcode.ticketsales.agent.core.conversation.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseTools purchaseTools;
    private final ConversationService conversationService;

    @PostMapping("/confirm")
    public AgentResponse confirm(@RequestParam(required = false) String conversationId, Authentication auth) {
        var data = new ResponseData();
        purchaseTools.confirmPurchase(auth.getName(), data);
        var response = data.toResponse(conversationId, "No hay nada por confirmar. Pídelo de nuevo.");
        conversationService.remember(auth.getName(), conversationId, "Sí, confirmo", response.message());
        return response;
    }

    @PostMapping("/reject")
    public AgentResponse reject(@RequestParam(required = false) String conversationId, Authentication auth) {
        purchaseTools.cancelPurchase(auth.getName());
        String message = "Listo, no se hizo ningún cambio.";
        conversationService.remember(auth.getName(), conversationId, "No, cancela", message);
        return AgentResponse.onlyMessage(conversationId, message);
    }
}
