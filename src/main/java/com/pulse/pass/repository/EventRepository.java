package com.pulse.pass.repository;

import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventCode(String eventCode);

    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

	default List<Event> findPublishedEvents() {
		return findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);
	}

    List<Event> findByVenue_Code(String venueCode);

    @Query("""
	    select distinct e
	    from Event e
	    join e.artists a
	    where a.stageName = :stageName
	    order by e.eventDate asc
	    """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    @Query("""
	    select distinct e
	    from Event e
	    join e.artists a
	    where lower(e.venue.city) = lower(:city)
	      and a.stageName = :stageName
	    order by e.eventDate asc
	    """)
    List<Event> findByVenueCityAndArtistStageName(
	    @Param("city") String city,
	    @Param("stageName") String stageName);

    @Query("""
	    select distinct e
	    from Event e
	    join e.artists a
	    where e.status = :status
	      and e.eventDate > :fromDate
	      and lower(e.venue.city) = lower(:city)
	      and lower(a.stageName) like lower(concat('%', :artistText, '%'))
	    order by e.eventDate asc
	    """)
    List<Event> findRecommendedEvents(
	    @Param("status") EventStatus status,
	    @Param("fromDate") LocalDateTime fromDate,
	    @Param("city") String city,
	    @Param("artistText") String artistText);
}
