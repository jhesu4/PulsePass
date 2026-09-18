package com.pulse.pass.repository;

import com.pulse.pass.domain.Ticket;
import com.pulse.pass.domain.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUser_EmailIgnoreCase(String email);

    List<Ticket> findByUser_EmailIgnoreCaseAndStatus(String email, TicketStatus status);

    List<Ticket> findByEvent_EventCodeAndStatus(String eventCode, TicketStatus status);

	default List<Ticket> findPaidByEventCode(String eventCode) {
		return findByEvent_EventCodeAndStatus(eventCode, TicketStatus.PAID);
	}

    @Query("""
	    select count(t)
	    from Ticket t
	    where t.event.eventCode = :eventCode
	      and t.status = :status
	    """)
    long countByEventCodeAndStatus(
	    @Param("eventCode") String eventCode,
	    @Param("status") TicketStatus status);

	    @Query("""
		    select count(t)
		    from Ticket t
		    where t.event.eventCode = :eventCode
		      and t.status = com.pulse.pass.domain.TicketStatus.PAID
		    """)
	    long countPaidByEventCode(@Param("eventCode") String eventCode);

    @Query("""
	    select t
	    from Ticket t
	    where t.event.eventDate > :fromDate
	    order by t.event.eventDate asc
	    """)
    List<Ticket> findForFutureEvents(@Param("fromDate") LocalDateTime fromDate);
}
