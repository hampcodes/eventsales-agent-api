package com.hampcode.ticketsales.repository;

import com.hampcode.ticketsales.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByDocumentId(String documentId);

    Optional<Participant> findByDocumentIdAndName(String documentId, String name);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
            UPDATE Participant p
            SET p.email = :email, p.phone = :phone
            WHERE p.id = :participantId
            """)
    int updateContactInfo(
            @Param("participantId") Long participantId,
            @Param("email") String email,
            @Param("phone") String phone
    );

    Optional<Participant> findByUser_Id(Long userId);
}
