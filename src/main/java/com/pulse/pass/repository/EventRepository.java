package com.pulse.pass.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventStatus;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventCode(String eventCode);

    boolean existsByEventCode(String eventCode);

    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    List<Event> findByVenueCode(String venueCode);

    // FR-SRC-001: Buscar eventos por artista
    @Query("SELECT DISTINCT e FROM Event e JOIN e.artists a WHERE a.stageName = :stageName")
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    // FR-SRC-002: Buscar eventos por ciudad del venue y nombre del artista
    @Query("SELECT DISTINCT e FROM Event e JOIN e.artists a WHERE e.venue.city = :city AND a.stageName = :stageName")
    List<Event> findByCityAndArtist(@Param("city") String city, @Param("stageName") String stageName);

    // FR-SRC-003: Eventos recomendados (PUBLISHED, fecha posterior, ciudad y búsqueda case-insensitive por artista)
    @Query("SELECT DISTINCT e FROM Event e JOIN e.artists a " +
           "WHERE e.status = com.pulse.pass.domain.EventStatus.PUBLISHED " +
           "AND e.eventDate > :afterDate " +
           "AND e.venue.city = :city " +
           "AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistName, '%')) " +
           "ORDER BY e.eventDate ASC")
    List<Event> findRecommendedEvents(
            @Param("afterDate") LocalDateTime afterDate,
            @Param("city") String city,
            @Param("artistName") String artistName
    );
}