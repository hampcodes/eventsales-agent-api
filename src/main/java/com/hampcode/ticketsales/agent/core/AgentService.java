package com.hampcode.ticketsales.agent.core;

import com.hampcode.ticketsales.agent.app.dto.AgentResponse;
import com.hampcode.ticketsales.agent.app.dto.ResponseData;
import com.hampcode.ticketsales.agent.core.conversation.ConversationService;
import com.hampcode.ticketsales.agent.core.routing.RoutingWorkflow;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final QuestionAnswerAdvisor knowledgeAdvisor;
    private final RoutingWorkflow routingWorkflow;
    private final ConversationService conversationService;
    private final List<AgentDefinition> definitions;

    // Si el enrutador duda, atiende el agente sin tools
    private static final String FALLBACK = "guard";

    private final Map<String, AgentDefinition> agents = new LinkedHashMap<>();
    private final Map<String, String> routes = new LinkedHashMap<>();
    private ChatClient chatClient;

    @PostConstruct
    void init() {
        chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
        definitions.forEach(agent -> {
            agents.put(agent.name(), agent);
            routes.put(agent.name(), agent.description());
        });
    }

    public AgentResponse chat(String userName, Set<String> roles, String message, String conversationId) {
        var conversation = conversationService.open(userName, conversationId, message);

        String name = InputGuard.isOffTopic(message) ? FALLBACK : routingWorkflow.route(message, routes, FALLBACK);
        var agent = agents.get(name);
        if (agent.requiredRole() != null && !roles.contains(agent.requiredRole())) {
            return AgentResponse.onlyMessage(conversation.getId(), "Esa opción no está disponible para tu rol.");
        }

        var data = new ResponseData();
        var request = chatClient.prompt()
                .system(prompt("common").formatted(LocalDate.now()) + prompt(name))
                .user(message)
                .toolContext(Map.of("user", userName, ResponseData.CONTEXT_KEY, data))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversation.getId()));
        if (agent.tools().length > 0) request.tools(agent.tools());
        if (agent.usesPdfs()) request.advisors(knowledgeAdvisor);

        return data.toResponse(conversation.getId(), OutputGuard.clean(request.call().content()));
    }

    private static String prompt(String name) {
        try {
            return new ClassPathResource("prompts/" + name + ".md").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
