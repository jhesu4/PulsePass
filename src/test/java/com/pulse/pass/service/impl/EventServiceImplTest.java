package com.pulse.pass.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulse.pass.domain.Artist;
import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.request.CreateEventRequest;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.DuplicateResourceException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.EventMapper;
import com.pulse.pass.repository.ArtistRepository;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.VenueRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private EventMapper eventMapper;

    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(eventRepository, venueRepository, artistRepository, eventMapper);
    }

    @Test
    void shouldReturnResponseWhenEventExists() {
        Event event = eventFor(activeVenue(true), EventStatus.DRAFT);
        EventResponse response = responseFor(EventStatus.DRAFT);
        when(eventRepository.findByEventCode("EVT-1")).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(response);

        assertThat(eventService.findByCode("EVT-1")).isSameAs(response);
    }

    @Test
    void shouldThrowWhenEventDoesNotExist() {
        when(eventRepository.findByEventCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByCode("MISSING"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: MISSING");
    }

    @Test
    void shouldCreateEventInDraftStatus() {
        Venue venue = activeVenue(true);
        CreateEventRequest request = validRequest();
        EventResponse response = responseFor(EventStatus.DRAFT);
        when(eventRepository.existsByEventCode(request.eventCode())).thenReturn(false);
        when(venueRepository.findByCode(request.venueCode())).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0, Event.class));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(response);

        assertThat(eventService.create(request)).isSameAs(response);

        verify(eventRepository)
                .save(org.mockito.ArgumentMatchers.argThat(event -> event.getStatus() == EventStatus.DRAFT
                        && event.getVenue() == venue
                        && event.getMinimumAge() == 18));
    }

    @Test
    void shouldRejectDuplicateEventCode() {
        CreateEventRequest request = validRequest();
        when(eventRepository.existsByEventCode(request.eventCode())).thenReturn(true);

        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Event already exists: EVT-1");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void shouldRejectCreationWhenVenueDoesNotExist() {
        CreateEventRequest request = validRequest();
        when(eventRepository.existsByEventCode(request.eventCode())).thenReturn(false);
        when(venueRepository.findByCode(request.venueCode())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found: VEN-1");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void shouldRejectCreationWhenVenueIsInactive() {
        CreateEventRequest request = validRequest();
        when(eventRepository.existsByEventCode(request.eventCode())).thenReturn(false);
        when(venueRepository.findByCode(request.venueCode())).thenReturn(Optional.of(activeVenue(false)));

        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot create an event in inactive venue: VEN-1");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void shouldRejectCreationWhenEventDateIsInThePast() {
        CreateEventRequest request = new CreateEventRequest(
                "EVT-1", "Event", "Description", EventCategory.MUSIC,
                LocalDateTime.now().minusDays(1), 18, "VEN-1");
        when(eventRepository.existsByEventCode(request.eventCode())).thenReturn(false);
        when(venueRepository.findByCode(request.venueCode())).thenReturn(Optional.of(activeVenue(true)));

        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Event date must be in the future.");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void shouldPublishFutureDraftEvent() {
        Event event = eventFor(activeVenue(true), EventStatus.DRAFT);
        EventResponse response = responseFor(EventStatus.PUBLISHED);
        when(eventRepository.findByEventCode("EVT-1")).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(response);

        assertThat(eventService.publish("EVT-1")).isSameAs(response);
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void shouldRejectPublishingEventThatIsNotDraft() {
        Event event = eventFor(activeVenue(true), EventStatus.CANCELLED);
        when(eventRepository.findByEventCode("EVT-1")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.publish("EVT-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only DRAFT events can be published: EVT-1");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void shouldRejectPublishingWhenVenueIsInactive() {
        Event event = eventFor(activeVenue(false), EventStatus.DRAFT);
        when(eventRepository.findByEventCode("EVT-1")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.publish("EVT-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot publish an event in inactive venue: VEN-1");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void shouldAddArtistToEvent() {
        Event event = eventFor(activeVenue(true), EventStatus.DRAFT);
        Artist artist = new Artist("Solar Beat", "Colombia", "Pop", true);
        artist.setId(7L);
        EventResponse response = responseFor(EventStatus.DRAFT);

        when(eventRepository.findByEventCode("EVT-1")).thenReturn(Optional.of(event));
        when(artistRepository.findById(7L)).thenReturn(Optional.of(artist));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(response);

        assertThat(eventService.addArtist("EVT-1", 7L)).isSameAs(response);
        assertThat(event.getArtists()).containsExactly(artist);

        verify(eventRepository).save(event);
    }

    @Test
    void shouldRejectDuplicatedArtistInEvent() {
        Event event = eventFor(activeVenue(true), EventStatus.DRAFT);
        Artist artist = new Artist("Solar Beat", "Colombia", "Pop", true);
        artist.setId(7L);
        event.getArtists().add(artist);

        when(eventRepository.findByEventCode("EVT-1")).thenReturn(Optional.of(event));
        when(artistRepository.findById(7L)).thenReturn(Optional.of(artist));

        assertThatThrownBy(() -> eventService.addArtist("EVT-1", 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Artist is already associated with event: EVT-1");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void shouldRejectAddingArtistToFinishedEvent() {
        Event event = eventFor(activeVenue(true), EventStatus.FINISHED);
        Artist artist = new Artist("Solar Beat", "Colombia", "Pop", true);
        artist.setId(7L);

        when(eventRepository.findByEventCode("EVT-1")).thenReturn(Optional.of(event));
        when(artistRepository.findById(7L)).thenReturn(Optional.of(artist));

        assertThatThrownBy(() -> eventService.addArtist("EVT-1", 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot add artists to event in status FINISHED: EVT-1");

        verify(eventRepository, never()).save(any(Event.class));
    }

    private CreateEventRequest validRequest() {
        return new CreateEventRequest(
                "EVT-1", "Event", "Description", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(5), 18, "VEN-1");
    }

    private Venue activeVenue(boolean active) {
        return new Venue("VEN-1", "Venue", "City", "Address", 100, active);
    }

    private Event eventFor(Venue venue, EventStatus status) {
        Event event = new Event();
        event.setEventCode("EVT-1");
        event.setName("Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(status);
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setMinimumAge(18);
        event.setVenue(venue);
        return event;
    }

    private EventResponse responseFor(EventStatus status) {
        return new EventResponse(
                1L, "EVT-1", "Event", "Description", EventCategory.MUSIC,
                status, LocalDateTime.now().plusDays(5), 18,
                "VEN-1", "Venue", List.of());
    }
}