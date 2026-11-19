package com.hampcode.ticketsales.agent.core.conversation;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "agent_conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Conversation {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
