package com.pulse.pass.service.impl;

import com.pulse.pass.domain.Artist;
import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.request.CreateEventRequest;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.dto.response.EventSummaryResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.DuplicateResourceException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.EventMapper;
import com.pulse.pass.repository.ArtistRepository;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.VenueRepository;
import com.pulse.pass.service.EventService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(
            EventRepository eventRepository,
            VenueRepository venueRepository,
            ArtistRepository artistRepository,
            EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event already exists: " + request.eventCode());
        }

        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));

        if (!venue.isActive()) {
            throw new BusinessRuleException("Cannot create an event in inactive venue: " + venue.getCode());
        }
        if (request.eventDate() == null || !request.eventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Event date must be in the future.");
        }
        if (request.minimumAge() == null || request.minimumAge() < 0) {
            throw new BusinessRuleException("Minimum age cannot be negative or null.");
        }

        Event event = new Event();
        event.setEventCode(request.eventCode());
        event.setName(request.name());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setEventDate(request.eventDate());
        event.setMinimumAge(request.minimumAge());
        event.setVenue(venue);
        event.setStatus(EventStatus.DRAFT);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse findByCode(String eventCode) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED).stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT events can be published: " + eventCode);
        }
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot publish an event whose date has passed: " + eventCode);
        }
        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException("Cannot publish an event in inactive venue: " + event.getVenue().getCode());
        }

        event.setStatus(EventStatus.PUBLISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + artistId));

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException("Cannot add artists to event in status " + event.getStatus() + ": " + eventCode);
        }
        if (event.getArtists().stream().anyMatch(existingArtist -> existingArtist.getId().equals(artistId))) {
            throw new BusinessRuleException("Artist is already associated with event: " + eventCode);
        }

        event.getArtists().add(artist);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findByArtistStageName(stageName).stream()
                .map(eventMapper::toSummary)
                .toList();
    }
}