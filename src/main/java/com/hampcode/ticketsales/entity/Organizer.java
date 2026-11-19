package com.hampcode.ticketsales.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "organizers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organizer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "document_id", nullable = false, unique = true)
    private String documentId;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String website;
}
