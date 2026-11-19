package com.hampcode.ticketsales.repository;

import com.hampcode.ticketsales.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findByEventId(Long eventId, Pageable pageable);

    Page<Ticket> findByParticipantId(Long participantId, Pageable pageable);

    List<Ticket> findByParticipantIdAndEventId(Long participantId, Long eventId);

    @Query("""
            SELECT t FROM Ticket t JOIN t.event e
            WHERE t.participant.id = :participantId
              AND LOWER(e.name) LIKE CONCAT('%', LOWER(:name), '%') ESCAPE '!'
            ORDER BY t.id DESC
            """)
    Page<Ticket> searchMine(@Param("participantId") Long participantId, @Param("name") String name, Pageable pageable);

    @Query("""
            SELECT t.status, COUNT(t)
            FROM Ticket t
            JOIN t.event e
            WHERE e.id = :eventId
            GROUP BY t.status
            """)
    List<Object[]> countTicketsByStatusForEvent(@Param("eventId") Long eventId);

    @Query(

            value = """
                    SELECT e.name, t.pricePaid, t.status
                    FROM Ticket t
                    JOIN t.event e
                    WHERE t.participant.id = :participantId
                    ORDER BY e.date DESC
                    """,

            countQuery = """
                    SELECT COUNT(t)
                    FROM Ticket t
                    WHERE t.participant.id = :participantId
                    """
    )
    Page<Object[]> findTicketHistoryByParticipant(@Param("participantId") Long participantId, Pageable pageable);

    @Query(
            value = """
                    SELECT e.name, COALESCE(SUM(t.price_paid), 0)
                    FROM tickets t
                    JOIN events e ON t.event_id = e.id
                    WHERE e.id = :eventId AND t.status = 'PAID'
                    GROUP BY e.name
                    """,
            nativeQuery = true
    )
    List<Object[]> sumRevenueByEvent(@Param("eventId") Long eventId);

    @Query(
            value = """
                    SELECT e.id, e.name, e.date, e.city, e.capacity_max, COUNT(t.id), COALESCE(SUM(t.price_paid), 0)
                    FROM events e
                    LEFT JOIN tickets t ON t.event_id = e.id AND t.status = 'PAID'
                    WHERE e.organizer_id = :organizerId
                    GROUP BY e.id, e.name, e.date, e.city, e.capacity_max
                    ORDER BY e.date
                    """,
            nativeQuery = true
    )
    List<Object[]> dashboardByOrganizer(@Param("organizerId") Long organizerId);

    @Query(
            value = """
                    SELECT e.name, e.date, e.city, e.capacity_max,
                           COUNT(t.id) FILTER (WHERE t.status = 'PAID'),
                           COUNT(t.id) FILTER (WHERE t.status = 'PENDING'),
                           COUNT(t.id) FILTER (WHERE t.status = 'CANCELLED'),
                           COALESCE(SUM(t.price_paid) FILTER (WHERE t.status = 'PAID'), 0)
                    FROM events e
                    LEFT JOIN tickets t ON t.event_id = e.id
                    WHERE e.organizer_id = :organizerId
                    GROUP BY e.id, e.name, e.date, e.city, e.capacity_max
                    ORDER BY e.date
                    """,
            nativeQuery = true
    )
    List<Object[]> salesReportByOrganizer(@Param("organizerId") Long organizerId);

    @Query(
            value = """
                    SELECT e.name, COUNT(t.id), COALESCE(SUM(t.price_paid), 0)
                    FROM events e
                    LEFT JOIN tickets t ON t.event_id = e.id AND t.status = 'PAID'
                    WHERE e.organizer_id = :organizerId
                    GROUP BY e.id, e.name
                    ORDER BY e.name
                    """,
            nativeQuery = true
    )
    List<Object[]> salesByEventForOrganizer(@Param("organizerId") Long organizerId);

    @Query(
            value = """
                    SELECT p.name, COALESCE(SUM(t.price_paid), 0)
                    FROM tickets t
                    JOIN participants p ON t.participant_id = p.id
                    WHERE p.id = :participantId AND t.status = 'PAID'
                    GROUP BY p.name
                    """,
            nativeQuery = true
    )
    List<Object[]> sumSpentByParticipant(@Param("participantId") Long participantId);

    @Query(
            value = "SELECT * FROM report_tickets_by_event(:eventId)",
            nativeQuery = true
    )
    List<Object[]> reportTicketsByEvent(@Param("eventId") Long eventId);

    @Query(
            value = "SELECT * FROM report_tickets_by_participant(:participantId)",
            nativeQuery = true
    )
    List<Object[]> reportTicketsByParticipant(@Param("participantId") Long participantId);
}
