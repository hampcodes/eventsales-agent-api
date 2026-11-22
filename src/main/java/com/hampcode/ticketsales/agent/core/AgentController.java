package com.hampcode.ticketsales.agent.core;

import com.hampcode.ticketsales.agent.app.dto.AgentResponse;
import com.hampcode.ticketsales.agent.core.conversation.ChatRequest;
import com.hampcode.ticketsales.agent.core.conversation.ConversationService;
import com.hampcode.ticketsales.agent.core.conversation.ConversationSummary;
import com.hampcode.ticketsales.agent.core.conversation.HistoryMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final ConversationService conversationService;
    private final AgentLimiter limiter;

    @PostMapping("/chat")
    public AgentResponse chat(@Valid @RequestBody ChatRequest request, Authentication auth) {
        return agentService.chat(auth.getName(), roles(auth), request.message(), request.conversationId());
    }

    @GetMapping("/conversations")
    public List<ConversationSummary> conversations(Authentication auth) {
        limiter.checkHistory(auth.getName());
        return conversationService.list(auth.getName());
    }

    @GetMapping("/conversations/{id}/messages")
    public List<HistoryMessage> messages(@PathVariable String id, Authentication auth) {
        limiter.checkHistory(auth.getName());
        return conversationService.messages(auth.getName(), id);
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable String id, Authentication auth) {
        conversationService.delete(auth.getName(), id);
    }

    private Set<String> roles(Authentication auth) {
        return auth.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", "")).collect(Collectors.toSet());
    }
}
