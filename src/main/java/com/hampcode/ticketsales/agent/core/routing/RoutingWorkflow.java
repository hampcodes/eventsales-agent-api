package com.hampcode.ticketsales.agent.core.routing;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RoutingWorkflow {

    private final ChatClient chatClient;

    public RoutingWorkflow(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    public String route(String message, Map<String, String> routes, String fallback) {
        String options = routes.entrySet().stream()
                .map(route -> route.getKey() + ": " + route.getValue())
                .collect(Collectors.joining("\n"));
        String system = "Elige el agente que debe atender el mensaje y responde solo con su nombre.\n\n"
                + options + "\n\nSi no estás seguro, responde " + fallback + ".";

        var decision = chatClient.prompt().system(system).user(message).call().entity(RouterDecision.class);
        return decision != null && routes.containsKey(decision.agent()) ? decision.agent() : fallback;
    }
}
