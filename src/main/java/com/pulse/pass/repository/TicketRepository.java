package com.pulse.pass.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pulse.pass.domain.Ticket;
import com.pulse.pass.domain.TicketStatus;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    // FR-TKT-006
    List<Ticket> findByUserEmail(String email);

    // FR-TKT-006
    List<Ticket> findByUserEmailAndStatus(String email, TicketStatus status);

    // FR-TKT-007
    @Query("SELECT t FROM Ticket t WHERE t.event.eventCode = :eventCode AND t.status = com.pulse.pass.domain.TicketStatus.PAID")
    List<Ticket> findPaidTicketsByEventCode(@Param("eventCode") String eventCode);

    // FR-TKT-008 (Defecto corregido: uso dinámico de :status)
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.eventCode = :eventCode AND t.status = :status")
    long countByEventCodeAndStatus(@Param("eventCode") String eventCode, @Param("status") TicketStatus status);

    // FR-SRC-004 (Requisito implementado)
    @Query("SELECT t FROM Ticket t WHERE t.event.eventDate > :date ORDER BY t.event.eventDate ASC")
    List<Ticket> findTicketsForEventsAfter(@Param("date") LocalDateTime date);
}