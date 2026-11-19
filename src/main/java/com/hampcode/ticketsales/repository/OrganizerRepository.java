package com.hampcode.ticketsales.repository;

import com.hampcode.ticketsales.entity.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizerRepository extends JpaRepository<Organizer, Long> {
    Optional<Organizer> findByDocumentId(String documentId);

    Optional<Organizer> findByDocumentIdAndName(String documentId, String name);

    Optional<Organizer> findByUser_Id(Long userId);
}
