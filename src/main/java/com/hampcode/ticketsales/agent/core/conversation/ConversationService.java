package com.hampcode.ticketsales.agent.core.conversation;

import com.hampcode.ticketsales.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ChatMemory chatMemory;

    public List<ConversationSummary> list(String userName) {
        return conversationRepository.findByUserNameOrderByUpdatedAtDesc(userName).stream()
                .map(conversation -> new ConversationSummary(conversation.getId(), conversation.getTitle()))
                .toList();
    }

    public List<HistoryMessage> messages(String userName, String conversationId) {
        return chatMemory.get(owned(userName, conversationId).getId()).stream()
                .filter(message -> message instanceof UserMessage || message instanceof AssistantMessage)
                .filter(message -> message.getText() != null && !message.getText().isBlank())
                .map(message -> new HistoryMessage(message instanceof UserMessage ? "usuario" : "agente", message.getText()))
                .toList();
    }

    public void delete(String userName, String conversationId) {
        var conversation = owned(userName, conversationId);
        chatMemory.clear(conversation.getId());
        conversationRepository.delete(conversation);
    }

    public Conversation open(String userName, String conversationId, String firstMessage) {
        Conversation conversation;
        if (conversationId == null || conversationId.isBlank()) {
            String title = firstMessage.trim().replaceAll("\\s+", " ");
            conversation = new Conversation(UUID.randomUUID().toString(), userName,
                    title.length() > 40 ? title.substring(0, 40) + "…" : title, Instant.now());
        } else {
            conversation = owned(userName, conversationId);
            conversation.setUpdatedAt(Instant.now());
        }
        return conversationRepository.save(conversation);
    }

    public void remember(String userName, String conversationId, String userText, String agentText) {
        if (conversationId == null || conversationId.isBlank()) return;
        conversationRepository.findByIdAndUserName(conversationId, userName).ifPresent(conversation -> {
            chatMemory.add(conversation.getId(), List.of(new UserMessage(userText), new AssistantMessage(agentText)));
            conversation.setUpdatedAt(Instant.now());
            conversationRepository.save(conversation);
        });
    }

    private Conversation owned(String userName, String conversationId) {
        return conversationRepository.findByIdAndUserName(conversationId, userName)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));
    }
}
