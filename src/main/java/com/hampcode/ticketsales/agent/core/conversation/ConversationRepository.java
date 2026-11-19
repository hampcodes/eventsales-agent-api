package com.hampcode.ticketsales.agent.core.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findByUserNameOrderByUpdatedAtDesc(String userName);

    Optional<Conversation> findByIdAndUserName(String id, String userName);
}
