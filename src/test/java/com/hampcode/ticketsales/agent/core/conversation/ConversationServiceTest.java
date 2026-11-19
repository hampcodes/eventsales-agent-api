package com.hampcode.ticketsales.agent.core.conversation;

import com.hampcode.ticketsales.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMemory chatMemory;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void listOnlyAsksForTheConversationsOfThatUser() {
        var own = new Conversation("c1", "3", "Mi conversación", Instant.now());
        when(conversationRepository.findByUserNameOrderByUpdatedAtDesc("3")).thenReturn(List.of(own));

        assertThat(conversationService.list("3")).containsExactly(new ConversationSummary("c1", "Mi conversación"));
        verify(conversationRepository, never()).findAll();
    }

    @Test
    void anotherUserCannotReadMessages() {
        when(conversationRepository.findByIdAndUserName("c1", "4")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.messages("4", "c1"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(chatMemory, never()).get("c1");
    }

    @Test
    void anotherUserCannotDeleteIt() {
        when(conversationRepository.findByIdAndUserName("c1", "4")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.delete("4", "c1"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(chatMemory, never()).clear("c1");
        verify(conversationRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void anotherUserCannotContinueItByPassingTheId() {
        when(conversationRepository.findByIdAndUserName("c1", "4")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.open("4", "c1", "hola"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(conversationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rememberIgnoresAConversationOfAnotherUser() {
        when(conversationRepository.findByIdAndUserName("c1", "4")).thenReturn(Optional.empty());

        conversationService.remember("4", "c1", "hola", "respuesta");

        verify(chatMemory, never()).add(org.mockito.ArgumentMatchers.eq("c1"), org.mockito.ArgumentMatchers.anyList());
    }
}
