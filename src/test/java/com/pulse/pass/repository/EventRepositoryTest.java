package com.pulse.pass.repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.TestcontainersConfiguration;
import com.pulse.pass.domain.Artist;
import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Venue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private ArtistRepository artistRepository;


    @Test
    @DisplayName("FR-VEN-001 / AC-001: Guardar y recuperar venue por código")
    void shouldSaveAndFindVenueByCode() {
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 10 #2-01", 5000, true);
        venueRepository.saveAndFlush(venue);

        Venue found = venueRepository.findByCode("VEN-SMR-01").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getCapacity()).isGreaterThan(0);
    }

    @Test
    @DisplayName("FR-VEN-002: Rechazar venue con código duplicado")
    void shouldRejectDuplicateVenueCode() {
        venueRepository.saveAndFlush(new Venue("VEN-DUP", "Venue A", "Cali", "Av 1", 1000, true));
        Venue dup = new Venue("VEN-DUP", "Venue B", "Cali", "Av 2", 2000, true);

        assertThatThrownBy(() -> venueRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-VEN-003: Rechazar venue con capacidad menor o igual a cero")
    void shouldRejectInvalidVenueCapacity() {
        Venue invalid = new Venue("VEN-INV", "Venue C", "Cali", "Av 3", 0, true);
        assertThatThrownBy(() -> venueRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-VEN-004: Consultar eventos por código de venue")
    void shouldFindEventsByVenueCode() {
        Venue venue = venueRepository.save(new Venue("VEN-EVT-01", "Arena SMR", "Santa Marta", "Av 5", 3000, true));
        Event event = createBaseEvent("EVT-V-01", "Evento Venue", EventStatus.PUBLISHED, venue);
        eventRepository.saveAndFlush(event);

        List<Event> events = eventRepository.findByVenueCode("VEN-EVT-01");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getVenue().getCode()).isEqualTo("VEN-EVT-01");
    }


    @Test
    @DisplayName("FR-EVT-001 / AC-002: Registrar evento y recuperar por eventCode con su venue")
    void shouldSaveAndFindByEventCode() {
        Venue venue = venueRepository.save(new Venue("VEN-CMF", "Marina Center", "Santa Marta", "Av 1", 5000, true));
        Event event = createBaseEvent("CMF-2026", "Caribbean Music Fest", EventStatus.PUBLISHED, venue);
        eventRepository.saveAndFlush(event);

        Event found = eventRepository.findByEventCode("CMF-2026").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getVenue().getName()).isEqualTo("Marina Center");
    }

    @Test
    @DisplayName("FR-EVT-002: Rechazar eventCode duplicado")
    void shouldRejectDuplicateEventCode() {
        Venue venue = venueRepository.save(new Venue("VEN-E1", "Arena 1", "Cali", "Av 1", 1000, true));
        eventRepository.saveAndFlush(createBaseEvent("EVT-DUP", "Evento 1", EventStatus.DRAFT, venue));

        Event dup = createBaseEvent("EVT-DUP", "Evento 2", EventStatus.PUBLISHED, venue);
        assertThatThrownBy(() -> eventRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-EVT-005 / AC-006: Consultar solo eventos PUBLISHED ordenados por fecha ascendente")
    void shouldFindPublishedEventsOrderedByDate() {
        Venue venue = venueRepository.save(new Venue("VEN-PUB", "Arena Pub", "Bogotá", "Av 2", 2000, true));
        Event e1 = createBaseEvent("EVT-P1", "Evento Futuro", EventStatus.PUBLISHED, venue);
        e1.setEventDate(LocalDateTime.now().plusDays(10));
        Event e2 = createBaseEvent("EVT-P2", "Evento Cercano", EventStatus.PUBLISHED, venue);
        e2.setEventDate(LocalDateTime.now().plusDays(2));
        Event e3 = createBaseEvent("EVT-D1", "Evento Borrador", EventStatus.DRAFT, venue);

        eventRepository.saveAllAndFlush(List.of(e1, e2, e3));

        List<Event> published = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);
        assertThat(published).extracting(Event::getEventCode).containsExactly("EVT-P2", "EVT-P1");
    }

    @Test
    @DisplayName("FR-EVT-006: Persistir y recuperar URL de streaming opcional")
    void shouldSaveAndRetrieveStreamingUrl() {
        Venue venue = venueRepository.save(new Venue("VEN-STR", "Arena Stream", "Medellín", "Av 3", 1000, true));
        Event event = createBaseEvent("EVT-STR-01", "Evento Híbrido", EventStatus.PUBLISHED, venue);
        event.setStreamingUrl("https://stream.pulsepass.com/live/123");
        eventRepository.saveAndFlush(event);

        Event found = eventRepository.findByEventCode("EVT-STR-01").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getStreamingUrl()).isEqualTo("https://stream.pulsepass.com/live/123");
    }


    @Test
    @DisplayName("FR-ART-001: Registrar nuevo artista y recuperar por stageName")
    void shouldSaveAndFindArtist() {
        Artist artist = new Artist("Astro Band", "Colombia", "Rock", true);
        artistRepository.saveAndFlush(artist);

        Artist found = artistRepository.findByStageName("Astro Band").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getCountry()).isEqualTo("Colombia");
    }

    @Test
    @DisplayName("FR-ART-002: Rechazar nombre artístico duplicado")
    void shouldRejectDuplicateStageName() {
        artistRepository.saveAndFlush(new Artist("Artist DUP", "Colombia", "Pop", true));
        Artist dup = new Artist("Artist DUP", "Chile", "Rock", true);

        assertThatThrownBy(() -> artistRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-ART-003 / AC-003: Asociar múltiples artistas a un evento sin duplicar relaciones")
    void shouldAssociateMultipleArtistsToEvent() {
        Venue venue = venueRepository.save(new Venue("VEN-ART", "Arena Art", "Medellín", "Av 4", 5000, true));
        Event event = createBaseEvent("EVT-ART-01", "Fest Multi", EventStatus.PUBLISHED, venue);

        Artist a1 = artistRepository.save(new Artist("Artist 1", "CO", "Pop", true));
        Artist a2 = artistRepository.save(new Artist("Artist 2", "AR", "Rock", true));
        Artist a3 = artistRepository.save(new Artist("Artist 3", "MX", "Indie", true));

        event.getArtists().addAll(List.of(a1, a2, a3));
        eventRepository.saveAndFlush(event);

        Event found = eventRepository.findByEventCode("EVT-ART-01").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getArtists()).hasSize(3).extracting(Artist::getStageName).contains("Artist 1", "Artist 2", "Artist 3");
    }


    @Test
    @DisplayName("FR-SRC-001 / AC-007: Buscar eventos por artista usando JOIN y DISTINCT")
    void shouldFindEventsByArtistStageName() {
        Venue venue = venueRepository.save(new Venue("VEN-SRC-1", "Arena SRC1", "Cali", "Av 1", 2000, true));
        Artist solarBeat = artistRepository.findByStageName("Solar Beat").orElseGet(() -> artistRepository.save(new Artist("Solar Beat", "CO", "Electronic", true)));

        Event e1 = createBaseEvent("EVT-SB-1", "Fest 1", EventStatus.PUBLISHED, venue);
        e1.getArtists().add(solarBeat);
        Event e2 = createBaseEvent("EVT-SB-2", "Fest 2", EventStatus.PUBLISHED, venue);
        e2.getArtists().add(solarBeat);

        eventRepository.saveAllAndFlush(List.of(e1, e2));

        List<Event> events = eventRepository.findByArtistStageName("Solar Beat");
        assertThat(events).hasSize(2).extracting(Event::getEventCode).contains("EVT-SB-1", "EVT-SB-2");
    }

    @Test
    @DisplayName("FR-SRC-002: Buscar eventos por ciudad y artista")
    void shouldFindEventsByCityAndArtist() {
        Venue venueSMR = venueRepository.save(new Venue("VEN-SMR-X", "Marina", "Santa Marta", "Av 1", 2000, true));
        Artist artist = artistRepository.save(new Artist("Luna", "CO", "Pop", true));

        Event e = createBaseEvent("EVT-CITY-1", "Fest SMR", EventStatus.PUBLISHED, venueSMR);
        e.getArtists().add(artist);
        eventRepository.saveAndFlush(e);

        List<Event> result = eventRepository.findByCityAndArtist("Santa Marta", "Luna");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Fest SMR");
    }

    @Test
    @DisplayName("FR-SRC-003: Buscar eventos recomendados (PUBLISHED, fecha posterior, ciudad y case-insensitive)")
    void shouldFindRecommendedEvents() {
    Venue venue = venueRepository.save(new Venue("VEN-REC", "Centro", "Barranquilla", "Av 5", 3000, true)); 
    Artist artist = artistRepository.findByStageName("Neon Waves")
            .orElseGet(() -> artistRepository.save(new Artist("Neon Waves", "CO", "Synth", true)));
            
    Event e = createBaseEvent("EVT-REC-1", "Neon Night", EventStatus.PUBLISHED, venue);
    e.setEventDate(LocalDateTime.now().plusDays(5));
    e.getArtists().add(artist);
    eventRepository.saveAndFlush(e);

    List<Event> recommended = eventRepository.findRecommendedEvents(LocalDateTime.now(), "Barranquilla", "neon");
    assertThat(recommended).hasSize(1);
    assertThat(recommended.get(0).getEventCode()).isEqualTo("EVT-REC-1");
}

    private Event createBaseEvent(String code, String name, EventStatus status, Venue venue) {
        Event event = new Event();
        event.setEventCode(code);
        event.setName(name);
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(status);
        event.setEventDate(LocalDateTime.now().plusDays(3));
        event.setMinimumAge(18);
        event.setVenue(venue);
        return event;
    }
}